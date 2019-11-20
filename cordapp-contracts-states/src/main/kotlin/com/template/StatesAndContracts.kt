package com.template

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.*
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.SecureHash
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable


// ************
// * Contract *
// ************
@CordaSerializable
class KaleContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.KaleContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.first()
        when (command.value){
            is Commands.Create-> requireThat{
                // Constraints on the shape of the transaction.
                "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
                "There should be one output state of type State." using (tx.outputs.size == 1)

                // IOU-specific constraints.
                val output = tx.outputsOfType<State>().single()
                "The sender and the receiver cannot be the same entity." using (output.sender != output.receiver)

                // Constraints on the signers.
                val expectedSigners = listOf(output.receiver.owningKey, output.sender.owningKey)
                "There must be two signers." using (command.signers.toSet().size == 2)
                "The receiver and sender must be signers." using (command.signers.containsAll(expectedSigners))
            }
            is Commands.Update-> requireThat {
                // Constraints on the shape of the transaction.
                "There should at least be One input consumed when issuing an IOU." using (tx.inputs.size == 1)
                "There should be one output state of type State." using (tx.outputs.size == 1)

                // IOU-specific constraints.
                val output = tx.outputsOfType<State>().single()
                "The sender and the receiver cannot be the same entity." using (output.sender != output.receiver)

                // Constraints on the signers.
                val expectedSigners = listOf(output.receiver.owningKey, output.sender.owningKey)
                "There must be two signers." using (command.signers.toSet().size == 2)
                "The receiver and sender must be signers." using (command.signers.containsAll(expectedSigners))
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
        class Update : Commands
    }
}

// *********
// * State *
// *********
@CordaSerializable
class State(val xmlHash: SecureHash,
               val fileName: String,
               val awbId: String,
               val uniqueMessageTypeNumber: String,
               val orgLoc: String,
               val dstLoc: String,
               val pkgs: String,
               val weight: String,
               val status: String,
               val timeStamp: String,
               val sender: Party,
               val receiver: Party) : ContractState {
    override val participants get() = listOf(sender, receiver)
}
