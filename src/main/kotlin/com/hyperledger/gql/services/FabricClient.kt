package com.hyperledger.gql.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.hyperledger.fabric.gateway.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.concurrent.TimeUnit


// https://github.com/hyperledgendary/fabric-rest-sample/blob/main/asset-transfer-basic/rest-api-typescript/src/fabric.ts

val ORG1_CONNECTION_PROFILE_PATH: String = System.getenv("ORG1_CONNECTION_PROFILE_PATH").toString() // org1_ccp.json
val ORG2_CONNECTION_PROFILE_PATH: String = System.getenv("ORG2_CONNECTION_PROFILE_PATH").toString() // org2_ccp.json
val ORG1_IDENTITY_FILE_PATH: String = System.getenv("ORG1_IDENTITY_FILE_PATH") // appuser_org1.id
val ORG2_IDENTITY_FILE_PATH: String = System.getenv("ORG2_IDENTITY_FILE_PATH") // appuser_org2.id
val CONFIG_FILE_PATH: String = System.getenv("CONFIG_FILE_PATH") // app-fabric-org1-v1-local-map || app-fabric-org1-v1-deployment-map
val yamlMapper = ObjectMapper(YAMLFactory())

class OrgIdentity {
    val credentials: Map<String, String> = mapOf<String, String>()
    val mspId: String = ""
    val type: String = ""
    val version: Int = 0
}

/**
 * ConfigKeys in a configmap must be camelcase
 *
 * TODO: Config File Class
 * **/
@JsonIgnoreProperties(ignoreUnknown = true)
class ConfigKeys {
    val logLevel: String = "" // app-fabric-org1-v1-map.yaml
    val hfcLogging: String = "" // app-fabric-org1-v1-map.yaml
    val retryDelay: String = "" // app-fabric-org1-v1-map.yaml
    val maxRetryCount: String = "" // app-fabric-org1-v1-map.yaml
    val hlfCommitTimeout: String = "" // app-fabric-org1-v1-map.yaml
    val hlfEndorseTimeout: String = "" // app-fabric-org1-v1-map.yaml
    val org1APIKey: String = "" // app-fabric-org1-v1-map.yaml ??
    val org2APIKey: String = "" // app-fabric-org1-v1-map.yaml ??
    val asLocalHost: String = "" // app-fabric-org1-v1-map.yaml
    val hlfChaincodeName: String = "" // app-fabric-org1-v1-map.yaml
    val channelName: String = "" // app-fabric-org1-v1-map.yaml
}

val configKeys: ConfigKeys = yamlMapper.readValue(
    File(CONFIG_FILE_PATH),
    ConfigKeys::class.java)

val org1Identity: OrgIdentity = yamlMapper.readValue(
    File(ORG1_IDENTITY_FILE_PATH),
    OrgIdentity::class.java
)

val org2Identity: OrgIdentity = yamlMapper.readValue(
    File(ORG2_IDENTITY_FILE_PATH),
    OrgIdentity::class.java
)

// Read as pure JSON string
val org1ConnectionProfile = String(Files.readAllBytes(Paths.get(ORG1_CONNECTION_PROFILE_PATH)))
val org2ConnectionProfile = String(Files.readAllBytes(Paths.get(ORG2_CONNECTION_PROFILE_PATH)))

class FabricClient {

    init {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true")
    }

    val wallet: Wallet = Wallets.newFileSystemWallet(Paths.get("/var", "hyperledger", "user", "wallet"))

    @Throws(Exception::class)
    private fun convertStringToX509Cert(certificate: String): X509Certificate {

        val cf = certificate.replace("\r\n", "")

        val targetStream: InputStream = ByteArrayInputStream(cf.toByteArray())
        return CertificateFactory
            .getInstance("X.509")
            .generateCertificate(targetStream) as X509Certificate
    }

    @Throws(Exception::class)
    private fun convertStringToPrivateKey(pem: String): PrivateKey {

        // Remove the "BEGIN" and "END" lines, as well as any whitespace
        val pkcs8Pem = pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")

        // Base64 decode the result
        val pkcs8EncodedBytes: ByteArray = Base64.getDecoder().decode(pkcs8Pem)

        // extract the private key
        val keySpec = PKCS8EncodedKeySpec(pkcs8EncodedBytes)
        val kf: KeyFactory = KeyFactory.getInstance("EC")
        return kf.generatePrivate(keySpec)
    }

    /**
      * Adds the admin Identities for the Org1, Org2 Admins
      *
      * In this sample there is a single user for each MSP ID to demonstrate how
      * a client app might submit transactions for different users
      *
      * TODO: Find a auth method that makes sure only authorized users of the admin OrgMsps can use admin functions
      **/
    fun registerAdminsToWallet(): Wallet {
        val org1 = Identities.newX509Identity(
            org1Identity.mspId,
            convertStringToX509Cert(org1Identity.credentials["certificate"]!!),
            convertStringToPrivateKey(org1Identity.credentials["privateKey"]!!)
        )
        wallet.put(org1Identity.mspId, org1)

        val org2 = Identities.newX509Identity(
            org2Identity.mspId,
            convertStringToX509Cert(org2Identity.credentials["certificate"]!!),
            convertStringToPrivateKey(org2Identity.credentials["privateKey"]!!)
        )
        wallet.put(org2Identity.mspId, org2)

        return wallet
    }

    /**
    * Create a Gateway connection
    *
    * Gateway instances can and should be reused rather than connecting to submit every transaction
    **/

    /**
    const options: GatewayOptions = {
        wallet,
        identity,
        discovery: { enabled: true, asLocalhost: config.asLocalhost },
        eventHandlerOptions: {
            commitTimeout: config.commitTimeout,
            endorseTimeout: config.endorseTimeout,
            strategy: DefaultEventHandlerStrategies.PREFER_MSPID_SCOPE_ANYFORTX,
        },
        queryHandlerOptions: {
                timeout: config.queryTimeout,
                strategy: DefaultQueryHandlerStrategies.PREFER_MSPID_SCOPE_ROUND_ROBIN,
            },
        };
    };
     * **/
    // TODO: Make sure the query Handlers work as they should, only query on peer1
    fun createGateway(organization: String, identity: String, wallet: Wallet): Gateway {
        // TODO: Can probably one line this
        // TODO: Add eventHandlerOptions

        val gatewayBuilder = when {
            (organization == "Org1") -> {
                Gateway.createBuilder()
                    .identity(wallet, identity)
                    .discovery(true)
                    .commitTimeout(configKeys.hlfCommitTimeout.toLong(), TimeUnit.MILLISECONDS)
                    .networkConfig(org1ConnectionProfile.byteInputStream())
                    .queryHandler(DefaultQueryHandlers.MSPID_SCOPE_SINGLE)
                    .commitHandler(DefaultCommitHandlers.MSPID_SCOPE_ANYFORTX)

            }
            (organization == "Org2") -> {
                Gateway.createBuilder()
                    .identity(wallet, identity)
                    .discovery(true)
                    .commitTimeout(configKeys.hlfCommitTimeout.toLong(), TimeUnit.MILLISECONDS)
                    .networkConfig(org2ConnectionProfile.byteInputStream())
                    .queryHandler(DefaultQueryHandlers.MSPID_SCOPE_SINGLE)
                    .commitHandler(DefaultCommitHandlers.MSPID_SCOPE_ANYFORTX)
            }
            else -> {
                throw Exception("No Organization Matches! Please check available organizations to create a gateway")
            }
        }

        return gatewayBuilder.connect()
    }

    /**
     * Get the network which the chaincode is running on
     *
     * In addion to getting the contract, the network will also be used to
     * start a block event listener
     *
     * **/
    fun getNetwork(gateway: Gateway): Network {
        return gateway.getNetwork(configKeys.channelName)
    }

    /**
     * Get the contract and the qscc system contract
     * @param: network: Network
     * @return: Pair<assetContract, gsccContract>
     **/
    fun getContracts(network: Network): Pair<Contract, Contract> {
        return Pair(network.getContract(configKeys.hlfChaincodeName), network.getContract("qscc"))
    }

    /**
     * Evaluate a transaction and handle any errors
     *
     * Used for querying the world state, transaction.evaluate runs the query on all endorser peers
     **/
    fun evaluateTransaction(contract: Contract, transactionName: String, vararg args: String): ByteArray {
//        val transaction = contract.evaluateTransaction(transactionName, *args)
//        val txnId = transaction.transactionId
        // logger (transaction, "Evaluating transaction...")

        try {
            return contract.evaluateTransaction(transactionName, *args)
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Submit a transaction and handle any errors
     *
     * Used to submit transactions such as transfer()
     **/
    fun submitTransaction(transaction: Transaction, vararg args: String): ByteArray {
        //logger.trace({ transaction }, 'Submitting transaction');
        val txnId = transaction.transactionId

        try {
            return transaction.submit(*args)
            // logger.trace(
            //                { transactionId: txnId, payload: payload.toString() },
            //                'Submit transaction response received'
            //            )

        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Get the validation code of the specified transaction
     **/
    fun getTransactionValidationCode(qsccContract: Contract, transactionId: String): String {
        val data = evaluateTransaction(
            qsccContract,
            "GetTransactionByID",
            configKeys.channelName,
            transactionId
        )

        val info = String(data, Charsets.UTF_8)
        println(info) // for debugging

        // logger.debug('Current block height: %d', blockHeight);
        return info
    }

    /**
     * Get the current block height
     *
     * This example of using a system contract is used for the liveness
     * endpoint
     **/
    fun getBlockHeight(qsccContract: Contract): String {

        val data = evaluateTransaction(
            qsccContract,
            "GetChainInfo",
            configKeys.channelName
        )
        val info = String(data, Charsets.UTF_8)
        println(info) // for debugging

        // logger.debug('Current block height: %d', blockHeight);
        return info
    }


}