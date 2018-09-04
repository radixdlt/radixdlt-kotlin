package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import org.junit.Assert.assertEquals
import org.junit.Test

class AtomBuilderTest {
    @Test
    fun testMultipleAtomPayloadBuildsShouldCreateSameAtom() {
        val atomBuilder = AtomBuilder()
            .setDataParticle(DataParticle(Payload("Hello".toByteArray()), "Test"))
            .addDestination(EUID(1))

        val atom1 = atomBuilder.build(0)
        val atom2 = atomBuilder.build(0)

        assertEquals(atom1.hash, atom2.hash)
    }
}
