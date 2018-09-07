package com.radixdlt.client.core.atoms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AtomTest {
    @Test
    fun testNullAtom() {
        val atom = Atom(null, emptyList(), emptySet(), null, null, 0)
        assertNull(atom.dataParticle)
        assertEquals(0, atom.abstractConsumables.size.toLong())
        assertEquals(0, atom.getConsumables().size.toLong())
        assertEquals(0, atom.getConsumers().size.toLong())
        assertNotNull(atom.hash)
        assertNotNull(atom.hid)
        assertNotNull(atom.summary())
        assertNotNull(atom.consumableSummary())
        assertEquals(0L, atom.timestamp)
        assertNotNull(atom.toString())

        assertEquals(atom, Atom(null, emptyList(), emptySet(), null, null, 0))
    }
}
