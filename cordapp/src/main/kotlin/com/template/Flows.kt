package com.template

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.transactions.SignedTransaction

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
@CordaSerializable
class Flows(val xmlHash: SecureHash,
            val fileName: String,
            val awbId: String,
            val uniqueMessageTypeNumber: String,
            val orgLoc: String,
            val dstLoc: String,
            val pkgs: String,
            val weight: String,
            val status: String,
            val timeStamp: String,
            val otherParty: Party) : FlowLogic<SignedTransaction>() {

    /** The progress tracker provides checkpoints indicating the progress of the flow to observers. */
    override val progressTracker = ProgressTracker()

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable
    override fun call():SignedTransaction {
        // We retrieve the notary identity from the network map.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        // We create the transaction components.
        val outputState = State(xmlHash,fileName,awbId,uniqueMessageTypeNumber,orgLoc,dstLoc,pkgs,weight,status,timeStamp, ourIdentity, otherParty)

        val command = Command(KaleContract.Commands.Create(), listOf(ourIdentity.owningKey, otherParty.owningKey))


        // We create a transaction builder and add the components.
        val txBuilder = TransactionBuilder(notary = notary)
                .addOutputState(outputState, KaleContract.ID)
                .addCommand(command)
                .addAttachment(xmlHash)

        // Verifying the transaction.
        txBuilder.verify(serviceHub)
        // We sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        // Creating a session with the other party.
        val otherPartySession = initiateFlow(otherParty)
        // Obtaining the counterparty's signature.
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(otherPartySession), CollectSignaturesFlow.tracker()))

        // We finalise the transaction.
        return subFlow(FinalityFlow(fullySignedTx))

    }
}

@InitiatedBy(Flows::class)
@CordaSerializable
class FlowsResponder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction." using (output is State)
            }
        }

        subFlow(signTransactionFlow)
    }
}