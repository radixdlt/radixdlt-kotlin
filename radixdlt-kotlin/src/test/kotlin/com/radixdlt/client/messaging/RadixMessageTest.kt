package com.radixdlt.client.messaging

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.Mockito.mock

class RadixMessageTest {
    @Test
    fun createReplyTest() {
        val to = mock(RadixAddress::class.java)
        val from = mock(RadixAddress::class.java)
        val content = RadixMessageContent(to, from, "Hello")
        val atom = mock(Atom::class.java)

        val radixMessage = RadixMessage(content, atom)
        val reply = radixMessage.createReply("Hi")
        assertTrue(reply.from === radixMessage.to)
        assertTrue(reply.to === radixMessage.from)
        assertEquals("Hi", reply.content)
    }
}