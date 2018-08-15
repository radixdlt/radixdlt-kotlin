package com.radixdlt.client.core.address

import com.radixdlt.client.core.crypto.ECPublicKey
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.DecoderException
import org.junit.Test

import java.math.BigInteger
import java.util.Arrays

import org.junit.Assert.assertEquals

class RadixAddressTest {

    @Test
    fun createAddressFromPublicKey() {
        val publicKey = ECPublicKey(Base64.decode("A455PdOZNwyRWaSWFXyYYkbj7Wv9jtgCCqUYhuOHiPLC"))
        val address = RadixAddress(RadixUniverseConfigs.winterfell, publicKey)
        assertEquals("JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ", address.toString())
        assertEquals(address, RadixAddress.fromString("JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ"))
    }

    @Test(expected = DecoderException::class)
    fun createAddressFromBadPublicKey() {
        val publicKey = ECPublicKey(Base64.decode("BADKEY"))
        RadixAddress(RadixUniverseConfigs.winterfell, publicKey)
    }

    @Test
    fun createAddressAndCheckUID() {
        val address = RadixAddress("JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ")
        assertEquals(EUID(BigInteger("-35592036731042511330623796977")), address.getUID())
    }

    @Test
    fun generateAddress() {
        RadixAddress(RadixUniverseConfigs.winterfell, ECPublicKey(ByteArray(33)))
    }

    @Test
    fun testAddresses() {
        val addresses = Arrays.asList(
                "JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ"
        )

        addresses.forEach { address -> RadixAddress.fromString(address) }
    }
}