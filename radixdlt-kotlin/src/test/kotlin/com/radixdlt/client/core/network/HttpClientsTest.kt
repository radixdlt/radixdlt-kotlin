package com.radixdlt.client.core.network

import org.junit.Assert.assertTrue
import org.junit.Test

class HttpClientsTest {
    @Test
    fun testClientCreation() {
        val client = HttpClients.getSslAllTrustingClient()
        for (i in 0..9) {
            assertTrue(client === HttpClients.getSslAllTrustingClient())
        }
    }
}