package com.radixdlt.client.core.serialization

import com.radixdlt.client.core.address.RadixUniverseConfig
import org.bouncycastle.util.encoders.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class DsonTest {
    @Test
    fun fromDsonTest() {
        val dson = Dson.instance
        val jsonElement = dson.parse(Base64.decode("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"))
        val universeFromDson = RadixJson.gson.fromJson(jsonElement, RadixUniverseConfig::class.java)
        assertEquals(-1014759422, universeFromDson.getMagic().toLong())
        assertEquals(3, universeFromDson.genesis.size.toLong())
    }
}
