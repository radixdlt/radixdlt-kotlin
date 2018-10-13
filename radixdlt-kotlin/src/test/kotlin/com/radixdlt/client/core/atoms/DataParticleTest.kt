package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.atoms.particles.DataParticle
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class DataParticleTest {
    @Test
    fun testApplicationMetaData() {
        val payload = mock(Payload::class.java)
        val dataParticle = DataParticle.DataParticleBuilder()
            .payload(payload)
            .setMetaData("application", "test")
            .build()
        assertEquals("test", dataParticle.getMetaData("application"))
        assertNull(dataParticle.getMetaData("missing"))
    }

    @Test
    fun testNullDataParticle() {
        assertThatThrownBy { DataParticle.DataParticleBuilder().setMetaData("application", "hello").build() }
            .isInstanceOf(NullPointerException::class.java)
    }
}
