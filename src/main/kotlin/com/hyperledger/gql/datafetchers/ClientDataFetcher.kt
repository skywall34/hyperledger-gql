package com.hyperledger.gql.datafetchers

import com.hyperledger.gql.Contracts
import com.hyperledger.gql.GATEWAY_ORG_1
import com.hyperledger.gql.fabricClient
import com.hyperledger.gql.logger
import com.hyperledger.gql.services.UserClient
import com.hyperledger.gql.services.org1Identity
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsQuery
import org.hyperledger.fabric.gateway.Contract
import java.util.concurrent.CompletableFuture

val userClient = UserClient()

/**
 * Interactive Query DataFetcher for Users
 *
 * The following should be implemented here and implemented in the required Service
 * - BalanceOf -> Returns the balance of the given account
 * - ClientAccountId -> Can get own clientId, or request the clientId of another user
 * - TotalSupply -> Returns the total supply of the given token contract (in this case coin-20)
 *
 * TODO: ENUM of available chaincode functions
 * TODO: Put evaluate contracts as a single common function with multiple return types
 * TODO: Allowance
 * **/
@DgsComponent
class ClientDataFetcher {

    private val clientGateway = fabricClient.createGateway(GATEWAY_ORG_1, org1Identity.mspId, fabricClient.wallet)
    private val clientNetwork = fabricClient.getNetwork(clientGateway)
    //         Pair<assetContract, gsccContract>
    private val clientContracts: Pair<Contract, Contract> = fabricClient.getContracts(clientNetwork)

    /**
     * total_supply
     *
     * @description: Returns the total number of coins minted
     * @return: TotalSupply: Long
     * **/
    @DgsQuery
    fun total_supply(
        dfe: DgsDataFetchingEnvironment
    ): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()

        try {
            logger.info("Calling TotalSupply...")
            val totalSupplyData = fabricClient.evaluateTransaction(clientContracts.first, Contracts.TOTAL_SUPPLY.contract)
            future.complete(String(totalSupplyData).toLong())
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
        return future
    }

    /**
     * balance_of
     *
     * @description: Returns the balance of the requested account
     * @param: account: String
     * @return: balance: Long
     * **/
    @DgsQuery
    fun balance_of(account: String, dfe: DgsDataFetchingEnvironment): CompletableFuture<Long> {
        val future = CompletableFuture<Long>()

        try {
            logger.info("Calling BalanceOf...")
            val balanceOfData = fabricClient.evaluateTransaction(clientContracts.first, Contracts.BALANCE_OF.contract, account)
            future.complete(String(balanceOfData).toLong())
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
        return future
    }

    /**
     * client_id
     *
     * The client id requires that the userGateway connected to it has the proper user,
     * so we define a temporary personal user's gateway and network here
     *
     * TODO: Find a way to utilize a common gateway for this function as well, maybe a job handler?
     *
     * @description: Returns the account id of the requested user
     * @return: account: String
     * **/
    @DgsQuery
    fun client_id(dfe: DgsDataFetchingEnvironment): CompletableFuture<String> {
        val username = userClient.getUser(dfe)
        val future = CompletableFuture<String>()

        logger.info("Connecting to Fabric with username received through $username...")
        val userGateway = fabricClient.createGateway(GATEWAY_ORG_1, username, fabricClient.wallet)
        val userNetwork = fabricClient.getNetwork(userGateway)
        try {
            val userContracts: Pair<Contract, Contract> = fabricClient.getContracts(userNetwork)
            logger.info("Calling clientAccountId...")
            val clientIdData = fabricClient.evaluateTransaction(userContracts.first, Contracts.CLIENT_ACCOUNT_ID.contract)
            future.complete(String(clientIdData))
        } catch (e: Exception) {
            future.completeExceptionally(e)
        } finally {
            userNetwork.gateway.close()
        }
        return future
    }
}