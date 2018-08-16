package com.radixdlt.client.messaging

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class RadixMessageTest {
    @Test
    fun toStringTest() {
        val from = mock(RadixAddress::class.java)
        val to = mock(RadixAddress::class.java)
        `when`<String>(from.toString()).thenReturn("Me")
        `when`<String>(to.toString()).thenReturn("You")
        val message = RadixMessage(from, to, "Hello", 0L)
        println(message.toString())
    }
}