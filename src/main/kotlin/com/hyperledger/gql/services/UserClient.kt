package com.hyperledger.gql.services

import com.hyperledger.gql.fabricClient
import com.hyperledger.gql.logger
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import org.hyperledger.fabric.gateway.Identities
import org.hyperledger.fabric.gateway.Identity
import org.hyperledger.fabric.gateway.X509Identity
import org.hyperledger.fabric.sdk.Enrollment
import org.hyperledger.fabric.sdk.User
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory
import org.hyperledger.fabric_ca.sdk.HFCAClient
import org.hyperledger.fabric_ca.sdk.RegistrationRequest
import java.security.PrivateKey
import java.util.*


// msp/organizations/peerOrganizations/org1.example.com/msp/cacerts/org1-ecert-ca.pem
//val CA_CLIENT_CERT_PATH: String = System.getenv("CA_CLIENT_CERT_PATH")
// msp/organizations/peerOrganizations/org1.example.com/msp/tlscacerts/org1-tls-ca.pem
val CA_TLS_CLIENT_CERT_PATH: String = System.getenv("CA_TLS_CLIENT_CERT_PATH")
// localhost:443 -> org1-ecert-ca:443
val ECERT_CA_SERVER: String = System.getenv("ECERT_CA_SERVER") ?: "https://localhost:7054"


class UserClient {
    // Handles Authorization, link usernames we get in our auth app with the wallets stored
    // Has an HashMap<String, Gateway> with key as the username and the value as the Gateway store
    // This HashMap represents current connected users. Removes the gateway on disconnect

    init {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true")
    }

    private fun registerUser(username: String) {

        // Create a CA client for interacting with the CA.
        try {
            val props = Properties()
            props["pemFile"] = CA_TLS_CLIENT_CERT_PATH
            props["allowAllHostNames"] = "true"
            val caClient = HFCAClient.createNewInstance(ECERT_CA_SERVER, props)
            val cryptoSuite = CryptoSuiteFactory.getDefault().cryptoSuite
            caClient.cryptoSuite = cryptoSuite

            // Check to see if we've already enrolled the user.
            if (fabricClient.wallet[username] != null) {
                println("An identity for the user $username already exists in the wallet")
                return
            }
            // Admin Identity is Org1MSP TODO: Refactor this code to use the Org1MSP User instead of Admin
            val adminIdentity = fabricClient.wallet[org1Identity.mspId] as X509Identity
            val admin: User = object : User {
                override fun getName(): String {
                    return "admin"
                }

                override fun getRoles(): Set<String>? {
                    return null
                }

                override fun getAccount(): String? {
                    return null
                }

                override fun getAffiliation(): String {
                    return "org1.department1"
                }

                override fun getEnrollment(): Enrollment {
                    return object : Enrollment {
                        override fun getKey(): PrivateKey {
                            return adminIdentity.privateKey
                        }

                        override fun getCert(): String {
                            return Identities.toPemString(adminIdentity.certificate)
                        }
                    }
                }

                override fun getMspId(): String {
                    return "Org1MSP"
                }
            }

            // Register the user, enroll the user, and import the new identity into the wallet.
            val registrationRequest = RegistrationRequest(username)
            registrationRequest.affiliation = "org1.department1"
            registrationRequest.enrollmentID = username

            val enrollmentSecret = caClient.register(registrationRequest, admin)
            val enrollment = caClient.enroll(username, enrollmentSecret)
            val user: Identity = Identities.newX509Identity("Org1MSP", enrollment)

            fabricClient.wallet.put(username, user)
            logger.info("Successfully enrolled user $username and imported it into the wallet")
        } catch (e: Exception) {
            throw Exception("Failed to register user!: ${e.message}, ${e.stackTraceToString()}")
        }

    }

    /**
     * getUser()
     * - Returns the username from the dfe header key authenticated or
     * - Returns the username from the local testUser env variable from the env.local.yaml set in Config
     * - If no user is found, register username as a guest user wallet
     * **/
    fun getUser(dfe: DgsDataFetchingEnvironment): String {
        // Simplify the username process for now, in the future we'd want this to be its own auth system
        // TODO: test security of this function, CA and wallet should handle authentication
        val username = dfe.getDgsContext().requestData!!.headers!!.getFirst("username")!!
        return if (fabricClient.wallet[username] != null) {
            username
        } else {
            registerUser(username)
            username
        }
    }

}