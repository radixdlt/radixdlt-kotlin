package com.radixdlt.client.core.network

import io.reactivex.Single
import okhttp3.OkHttpClient
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpClients {

    /**
     * Lock for http client
     */
    private val LOCK = Any()

    /**
     * Single OkHttpClient to be used for all connections
     */
    private var sslAllTrustingClient: OkHttpClient? = null


    private fun createClient(trustManager: (Array<X509Certificate>, String) -> Single<Boolean>): OkHttpClient {
        // TODO: Pass trust issue to user
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                if (!trustManager(chain, authType).blockingGet()) {
                    throw CertificateException()
                }
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if (!trustManager(chain, authType).blockingGet()) {
                    throw CertificateException()
                }
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                return arrayOf()
            }
        })

        try {
            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { hostname, session -> hostname == session.peerHost }

            builder.connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .pingInterval(30, TimeUnit.SECONDS)

            return builder.build()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Could not create http client: " + e.message)
        } catch (e: KeyManagementException) {
            throw RuntimeException("Could not create http client: " + e.message)
        }
    }

    /**
     * Builds OkHttpClient to be used for secure connections with self signed
     * certificates.
     */
    @JvmStatic
    fun getSslAllTrustingClient(): OkHttpClient {
        synchronized(LOCK) {
            if (sslAllTrustingClient == null) {
                sslAllTrustingClient = createClient { _, _ -> Single.just(true) }
            }
            return sslAllTrustingClient!!
        }
    }
}
