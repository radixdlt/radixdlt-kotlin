package com.radixdlt.client.core.atoms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtomTest {
    @Test
    fun testNullAtom() {
        val atom = Atom(null, emptyList(), emptyList(), emptySet(), null, null, 0)
        assertTrue(atom.dataParticles!!.isEmpty())
        assertTrue(atom.consumables!!.isEmpty())
        assertTrue(atom.consumers!!.isEmpty())
        assertNotNull(atom.hash)
        assertNotNull(atom.hid)
        assertNotNull(atom.summary())
        assertNotNull(atom.consumableSummary())
        assertEquals(0L, atom.timestamp)
        assertNotNull(atom.toString())

        assertEquals(atom, Atom(null, emptyList(), emptyList(), emptySet(), null, null, 0))
    }
}