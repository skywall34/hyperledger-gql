package com.hyperledger.gql.mutations

import com.hyperledger.gql.Contracts
import com.hyperledger.gql.GATEWAY_ORG_1
import com.hyperledger.gql.datafetchers.userClient
import com.hyperledger.gql.fabricClient
import com.hyperledger.gql.logger
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsMutation
import org.hyperledger.fabric.gateway.Contract
import java.util.concurrent.CompletableFuture

@DgsComponent
class ClientMutation {

    /**
     * transfer
     *
     * @description: Transfer transfers tokens from client account to recipient account
     * recipient account must be a valid clientID as returned by the ClientID() function
     *
     * @param: recepient: String -> clientId of the person receiving coin
     * @param: amount: Int -> amount of coin to be sent
     * @return: results: String -> Basic Transaction Complete message saying the transaction is complete
     *
     * TODO: Common job handler so no need to instantiate a client for each transaction to get the user wallet
     * **/
    @DgsMutation
    fun transfer(recipient: String, amount: Int, dfe: DgsDataFetchingEnvironment): CompletableFuture<String> {
        val username = userClient.getUser(dfe)
        val future = CompletableFuture<String>()

        logger.info("Connecting to Fabric with username received through $username...")
        val userGateway = fabricClient.createGateway(GATEWAY_ORG_1, username, fabricClient.wallet)
        val userNetwork = fabricClient.getNetwork(userGateway)
        try {
            val userContracts: Pair<Contract, Contract> = fabricClient.getContracts(userNetwork)
            logger.info("Calling Transfer...")
            val transaction = userContracts.first.createTransaction(Contracts.TRANSFER.contract)
            val transactionResult = fabricClient.submitTransaction(transaction, recipient, amount.toString())
            logger.info("Transaction Result: ${String(transactionResult)}")
            future.complete("Transaction Complete!")
        } catch (e: Exception) {
            future.completeExceptionally(e)
        } finally {
            userNetwork.gateway.close()
        }
        return future

    }
}