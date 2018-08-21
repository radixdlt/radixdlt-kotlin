package com.radixdlt.client.core.crypto

import com.radixdlt.client.core.util.AndroidUtil
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.SecureRandom
import java.security.Security
import java.util.Arrays

class ECKeyPairGenerator private constructor() {

    private val secureRandom = SecureRandom()

    // Generates a new Public/Private Key pair
    @JvmOverloads
    fun generateKeyPair(numBits: Int = 256): ECKeyPair {
        try {
            val g2 = KeyPairGenerator.getInstance("EC", "BC")
            val domain = getDomain(numBits)
            val curveSpec = ECParameterSpec(domain?.curve, domain?.g, domain?.n, domain?.h)
            g2.initialize(curveSpec, secureRandom)
            val keypair = g2.generateKeyPair()
            var privateKey = (keypair
                .private as org.bouncycastle.jce.interfaces.ECPrivateKey).d.toByteArray()

            if (privateKey.size != numBits / 8) {
                // Remove signed byte
                privateKey = if (privateKey.size == numBits / 8 + 1 && privateKey[0].toInt() == 0) {
                    Arrays.copyOfRange(privateKey, 1, privateKey.size)
                } else if (privateKey.size < numBits / 8) { // Pad
                    val copy = ByteArray(32)
                    System.arraycopy(privateKey, 0, copy, 32 - privateKey.size, privateKey.size)
                    copy
                } else {
                    throw RuntimeException()
                }
            }

            val publicKey = (keypair.public as org.bouncycastle.jce.interfaces.ECPublicKey).q.getEncoded(true)
            return ECKeyPair(publicKey, privateKey)
        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException(e.message)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e.message)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException(e.message)
        }
    }

    companion object {
        private val DOMAINS: Map<Int, ECDomainParameters>

        init {
            if (AndroidUtil.isAndroidRuntime) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            }
            Security.insertProviderAt(BouncyCastleProvider(), 1)

            DOMAINS = sequenceOf(256).associateBy({ Integer.valueOf(it) }) { bits: Int ->
                val curve = when (bits) {
                    256 -> CustomNamedCurves.getByName("secp" + bits + "k1")
                    else -> SECNamedCurves.getByName("secp" + bits + "k1")
                }
                return@associateBy ECDomainParameters(curve.curve, curve.g, curve.n, curve.h)
            }
        }

        @JvmStatic
        fun getDomain(numBits: Int): ECDomainParameters? {
            val roundedNumBits = ((numBits - 1) / 32 + 1) * 32
            return DOMAINS[roundedNumBits]
        }

        @JvmStatic
        fun newInstance(): ECKeyPairGenerator {
            return ECKeyPairGenerator()
        }
    }
}
