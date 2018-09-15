package com.radixdlt.client.core.serialization

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Tests for utility methods in [SerializationUtils].
 */
class SerializationUtilsTest {

    /**
     * Here we test all the size boundaries to make sure they
     * are as expected.
     */
    @Test
    fun testIntLength() {
        // Test boundary cases for each encoding length.
        assertEquals(1, SerializationUtils.intLength(0))
        assertEquals(1, SerializationUtils.intLength(159))
        assertEquals(2, SerializationUtils.intLength(160))
        assertEquals(2, SerializationUtils.intLength(8191))
        assertEquals(3, SerializationUtils.intLength(8192))
        assertEquals(4, SerializationUtils.intLength(2097152))
        assertEquals(4, SerializationUtils.intLength(SerializationUtils.SERIALIZE_MAX_INT))

        assertThatThrownBy { SerializationUtils.intLength(-1) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { SerializationUtils.intLength(SerializationUtils.SERIALIZE_MAX_INT + 1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    /**
     * Here we test every fourth serializable integer
     * to make sure that we can complete a round trip.
     */
    @Test
    fun testIntEncodeDecode() {
        val bytes = ByteBuffer.allocate(0x10)
        var i = 0
        while (i < SerializationUtils.SERIALIZE_MAX_INT) {
            val intLength = SerializationUtils.intLength(i)
            bytes.clear()
            SerializationUtils.encodeInt(i, bytes)
            assertEquals(intLength.toLong(), bytes.position().toLong())
            bytes.flip()
            assertEquals(i, SerializationUtils.decodeInt(bytes))
            i += 4
        }
    }
}
