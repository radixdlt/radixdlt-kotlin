package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class AtomBuilderTest {
    @Test
    fun buildTransactionAtomWithPayload() {
        val ecKeyPair = mock(ECKeyPair::class.java)
        `when`(ecKeyPair.getUID()).thenReturn(EUID(1))

        val consumable = Consumable(1, setOf(ecKeyPair), 0, EUID(2))

        val atomBuilder = AtomBuilder()
        val atom = atomBuilder
            .addParticle(consumable)
            .payload("Hello")
            .build()

        assertEquals(atom.rawAtom.payload!!.toAscii(), "Hello")
    }

    @Test
    fun testMultipleAtomPayloadBuildsShouldCreateSameAtom() {
        val atomBuilder = AtomBuilder()
            .applicationId("Test")
            .payload("Hello")
            .addDestination(EUID(1))

        val atom1 = atomBuilder.build()
        val atom2 = atomBuilder.build()

        assertEquals(atom1.hash, atom2.hash)
    }
}
