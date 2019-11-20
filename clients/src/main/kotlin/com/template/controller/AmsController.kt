package com.template.webserver

import com.template.State
import com.template.Flows
import com.template.UpdateFlows
import com.template.config.NodeRPCConnection
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import net.corda.core.node.services.vault.SortAttribute
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.multipart.MultipartFile
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import java.util.UUID;
import kotlin.IllegalArgumentException


/**
 * Define your API endpoints here.
 */
@RestController("AmsController")
@RequestMapping("/amsAirport") // The paths for HTTP requests are relative to this base path.
@CordaSerializable
class AmsController(@Qualifier("amsAirportConnection") rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy = rpc.proxy

    data class Information(val airwayBillNumber: String,
                           val uniqueMessageTypeNumber: String,
                           val originalLocation: String,
                           val finalDestinationLocation: String,
                           val pkgs: String,
                           val weight: String,
                           val status : String,
                           val timeStamp: String)
    /**
     * Get the payload by message type unique number which we have romdomly created.
     */
    @GetMapping(value = "getPayloadByMessageTypeNum")
    private fun getPayloadByMessageTypeNum(@RequestParam(value = "airwayBillNumber") airwayBillNumber: String, @RequestParam(value = "uniqueMessageTypeNum") uniqueMessageTypeNum: String): ByteArray {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)

        val pageResult = PageSpecification(1, Int.MAX_VALUE-1)
        val xmlHash = proxy.vaultQueryBy<State>(generalCriteria, paging = pageResult).states.filter { it.state.data.awbId == airwayBillNumber && it.state.data.uniqueMessageTypeNumber == uniqueMessageTypeNum }.map { it.state.data.xmlHash }

        var info: ByteArray = ByteArray(1)
        return if (xmlHash.isNotEmpty()) {
            val zipFile = proxy.openAttachment(xmlHash.first())
            val zip = ZipInputStream(zipFile)
            zip.nextEntry
            info += zip.readBytes()
            info
        } else {
            throw IllegalArgumentException("The file does NOT exists with this unique message type number.")
        }
    }

    /**
     * Displays all XML IOU states that exist in the node's vault(ALL) with airway bill number ID. filtered ONLY to return XML payload
     */
    @GetMapping(value = "getAllTransactionsById")
    private fun getAllTxOne(@RequestParam(value = "airwayBillNumber") airwayBillNumber: String): List<Information> {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)

        val sortColumn = Sort.SortColumn(SortAttribute.Standard(Sort.VaultStateAttribute.RECORDED_TIME), Sort.Direction.DESC)
        val sorting = Sort(listOf(sortColumn))
        val pageResult = PageSpecification(1, Int.MAX_VALUE-1)
        val response = proxy.vaultQueryBy<State>(generalCriteria,paging = pageResult, sorting = sorting).states.filter { it.state.data.awbId == airwayBillNumber }.map { it.state.data }

        var allInformation = mutableListOf<Information>()
        return if (response.isNotEmpty()){
            for (state in response) {
                val information = Information(state.awbId, state.uniqueMessageTypeNumber,state.orgLoc, state.dstLoc, state.pkgs, state.weight, state.status, state.timeStamp)
                allInformation.add(information)
            }
            allInformation
        } else{
            throw IllegalArgumentException("There is NO entry with this airway number")
        }
    }

    /**
     * Displays all XML IOU states that exist in the node's vault(UNCONSUMED) with airway bill number ID. filtered ONLY to return XML payload
     */
    @GetMapping(value = "getTransactionById")
    private fun getTxDetailsOne(@RequestParam(value = "airwayBillNumber") airwayBillNumber: String): Information {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)

        val pageResult = PageSpecification(1, Int.MAX_VALUE-1)
        val response = proxy.vaultQueryBy<State>(generalCriteria, paging = pageResult).states.filter { it.state.data.awbId == airwayBillNumber }.map { it.state.data }

        if (response.isNotEmpty()) {
            return Information(response.first().awbId, response.first().uniqueMessageTypeNumber, response.first().orgLoc, response.first().dstLoc, response.first().pkgs, response.first().weight, response.first().status, response.first().timeStamp)
        } else {
            throw IllegalArgumentException("There is NO entry with this airway number")
        }
    }

    /**
     * Create IOU's having AirWay Bill number as an ID and XML Payload in requestBody against the desired Party.
     */
    @PostMapping(value = "createTransaction")
    private  fun createTransactionOne(@RequestBody file : MultipartFile, @RequestParam otherParty: String): ResponseEntity<String> {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val xmlInput = InputSource(file.inputStream)
        val doc = dBuilder.parse(xmlInput)
        doc.documentElement.normalize()

        println(doc.documentElement.nodeName)
        val awbId = getXmlValues(doc, "TransportContractDocument", "ID")
        val orgLoc = getXmlValues(doc, "OriginLocation", "ID")
        val dstLoc= getXmlValues(doc, "FinalDestinationLocation", "ID")
        val pkgs = getXmlValues(doc, "AssociatedStatusConsignment", "PieceQuantity")
        val weight = getXmlValues(doc, "ns2:MasterConsignment", "GrossWeightMeasure")
        val status = getXmlValues(doc, "ReportedStatus", "ReasonCode")
        val timeStamp = getXmlValues(doc, "ns2:MessageHeaderDocument", "IssueDateTime")

        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            val zos = ZipOutputStream(byteArrayOutputStream)
            println(file.originalFilename)
            val zipEntry =  ZipEntry(file.originalFilename)
            zos.putNextEntry(zipEntry)
            zos.write(file.bytes)
            zos.closeEntry()
            zos.close()
        } catch (ioe: IOException){
            ioe.printStackTrace()
        }

        val zipfile = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        val hash = proxy.uploadAttachment(zipfile)
        val fileName = file.originalFilename

        var uuid = UUID.randomUUID()
        var uniqueMessageTypeNumber = uuid.toString()

        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL)
        val pageResult = PageSpecification(1, Int.MAX_VALUE-1)
        val response = proxy.vaultQueryBy<State>(generalCriteria, paging = pageResult).states.filter { it.state.data.awbId == awbId }.map { it.state.data }
        val partyX500Name = CordaX500Name.parse(otherParty)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $otherParty cannot be found.\n")

        if (response.isEmpty()){
            return try {
                val signedTx = proxy.startFlowDynamic(Flows::class.java, hash, fileName,awbId, uniqueMessageTypeNumber, orgLoc,dstLoc,pkgs,weight, status, timeStamp,otherParty).returnValue.getOrThrow()
                ResponseEntity.status(HttpStatus.CREATED).body("{$signedTx}")
            } catch (ex: Throwable) {
                logger.error(ex.message, ex)
                ResponseEntity.badRequest().body(ex.message!!)
            }
        } else {
            return try {
                val signedTx = proxy.startFlowDynamic(UpdateFlows::class.java, hash, fileName,awbId,uniqueMessageTypeNumber, orgLoc,dstLoc,pkgs,weight, status, timeStamp,otherParty).returnValue.getOrThrow()
                ResponseEntity.status(HttpStatus.CREATED).body("{$signedTx}")
            } catch (ex: Throwable) {
                logger.error(ex.message, ex)
                ResponseEntity.badRequest().body(ex.message!!)
            }
        }
    }

    private fun getXmlValues(doc: Document, docTagName: String, elementTagName: String) : String{
        var defaultValue: String = "0"

        return try {
            var docTag = doc.getElementsByTagName(docTagName)
            var node = docTag.item(0)
            var elem = node as Element
            var response = elem?.getElementsByTagName(elementTagName).item(0).textContent
            return if (response.isNotEmpty()) {
                response
            } else {
                return defaultValue;
            }
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            throw IllegalArgumentException("The XML is invalid- The mandatory <$elementTagName> tag is missing in <$docTagName> tags.")
        }
    }
}