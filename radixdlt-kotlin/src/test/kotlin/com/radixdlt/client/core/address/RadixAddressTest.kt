package com.radixdlt.client.core.address

import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.util.Int128
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.DecoderException
import org.bouncycastle.util.encoders.Hex
import org.junit.Assert.assertEquals
import org.junit.Test

class RadixAddressTest {

    @Test
    fun createAddressFromPublicKey() {
        val publicKey = ECPublicKey(Base64.decode("A455PdOZNwyRWaSWFXyYYkbj7Wv9jtgCCqUYhuOHiPLC"))
        val address = RadixAddress(RadixUniverseConfigs.betanet, publicKey)
        assertEquals("JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ", address.toString())
        assertEquals(address, RadixAddress.fromString("JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ"))
    }

    @Test(expected = DecoderException::class)
    fun createAddressFromBadPublicKey() {
        val publicKey = ECPublicKey(Base64.decode("BADKEY"))
        RadixAddress(RadixUniverseConfigs.betanet, publicKey)
    }

    @Test
    fun createAddressAndCheckUID() {
        val address = RadixAddress("JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ")
        assertEquals(EUID(Int128.from(Hex.decode("8cfef50ea6a767813631490f9a94f73f"))), address.getUID())
    }

    @Test
    fun generateAddress() {
        RadixAddress(RadixUniverseConfigs.betanet, ECPublicKey(ByteArray(33)))
    }

    @Test
    fun testAddresses() {
        val addresses = listOf(
                "JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ"
        )

        addresses.forEach { address -> RadixAddress.fromString(address) }
    }
}
