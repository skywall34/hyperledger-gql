package com.hyperledger.gql

import com.hyperledger.gql.services.FabricClient
import com.hyperledger.gql.services.org1Identity
import org.hyperledger.fabric.gateway.Contract
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

const val TOTAL_SUPPLY = "10000000000"
const val GATEWAY_ORG_1 = "Org1"
const val GATEWAY_ORG_2 = "Org2"

// command messages
enum class Contracts(val contract: String) {
    TOTAL_SUPPLY("TotalSupply"),
    BALANCE_OF("BalanceOf"),
    CLIENT_ACCOUNT_ID("ClientAccountID"),
    ALLOWANCE("Allowance"),
    TRANSFER("Transfer")
}

@SpringBootApplication
open class GraphqlApplication

val fabricClient = FabricClient()
val logger = LoggerFactory.getLogger("com.hyperledger.gql")

fun main(args: Array<String>) {

    try {
        logger.info("Registering admins to wallet")
        fabricClient.registerAdminsToWallet()

        logger.info("Connecting to Fabric with org1 mspid")
        val gatewayOrg1 = fabricClient.createGateway(GATEWAY_ORG_1, org1Identity.mspId, fabricClient.wallet)
        val networkOrg1 = fabricClient.getNetwork(gatewayOrg1)
//         Pair<assetContract, gsccContract>
        val contractsOrg1: Pair<Contract, Contract> = fabricClient.getContracts(networkOrg1)

        logger.info("Test a simple TotalSupply call to make sure the connection is good")
        val org1TotalSupplyData = fabricClient.evaluateTransaction(contractsOrg1.first, Contracts.TOTAL_SUPPLY.contract)

        if (org1TotalSupplyData.isNotEmpty()) {
            // validate that the both organizations have access and can make calls
            if (String(org1TotalSupplyData) != TOTAL_SUPPLY) {
                throw Exception("Call to TotalSupply for coin-20 failed! Received: ${String(org1TotalSupplyData)}" +
                        " Expected: $TOTAL_SUPPLY")
            }
        }
        // Make sure you close any open gateways
        gatewayOrg1.close()

        //TODO: Test Register User functionality
        //TODO: Create Jobs Class
    } catch (e: Exception) {
        throw Exception("Failed to initialize application! ${e.message}, ${e.stackTraceToString()}")
    }
    runApplication<GraphqlApplication>(*args)

}
