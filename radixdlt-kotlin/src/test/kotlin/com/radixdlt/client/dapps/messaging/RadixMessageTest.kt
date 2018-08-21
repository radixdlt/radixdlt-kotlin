package com.radixdlt.client.dapps.messaging

import com.radixdlt.client.core.address.RadixAddress
import org.junit.Test

import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class RadixMessageTest {
    @Test
    fun toStringTest() {
        val from = mock(RadixAddress::class.java)
        val to = mock(RadixAddress::class.java)
        `when`<String>(from.toString()).thenReturn("Me")
        `when`<String>(to.toString()).thenReturn("You")
        val message = RadixMessage(from, to, "Hello", 0L, true)
        println(message.toString())
    }
}