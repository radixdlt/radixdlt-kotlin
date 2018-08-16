package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.math.BigInteger

class AtomBuilderTest {
    @Test
    fun buildTransactionAtomWithPayload() {
        val ecKeyPair = mock(ECKeyPair::class.java)
        `when`(ecKeyPair.getUID()).thenReturn(EUID(1))

        val consumable = Consumable(1, setOf(ecKeyPair), 0, EUID(2))

        val atomBuilder = AtomBuilder()
        val atom = atomBuilder
                .type(TransactionAtom::class.java)
                .addParticle(consumable)
                .payload("Hello")
                .build()

        assertEquals(atom.rawAtom.javaClass, TransactionAtom::class.java)
        assertEquals(atom.rawAtom.asTransactionAtom.encrypted!!.toAscii(), "Hello")
    }

    @Test
    fun testMultipleAtomPayloadBuildsShouldCreateSameAtom() {
        val atomBuilder = AtomBuilder()
                .type(ApplicationPayloadAtom::class.java)
                .applicationId("Test")
                .payload("Hello")
                .addDestination(EUID(1))

        val atom1 = atomBuilder.build()
        val atom2 = atomBuilder.build()

        assertEquals(atom1.hash, atom2.hash)
    }
}
