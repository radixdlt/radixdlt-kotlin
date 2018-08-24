package com.radixdlt.client.core.atoms

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class DataParticleTest {
    @Test
    fun testApplicationMetaData() {
        val payload = mock(Payload::class.java)
        val dataParticle = DataParticle(payload, "test")
        assertEquals("test", dataParticle.getMetaData("application"))
        assertNull(dataParticle.getMetaData("missing"))
    }

    @Test
    fun testNullDataParticle() {
        assertThatThrownBy { DataParticle(null, "hello") }
            .isInstanceOf(NullPointerException::class.java)
    }
}

