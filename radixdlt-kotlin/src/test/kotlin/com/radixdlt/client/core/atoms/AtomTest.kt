package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.atoms.particles.Spin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtomTest {
    @Test
    fun testNullAtom() {
        val atom = Atom(null)
        assertTrue(atom.getDataParticles()!!.isEmpty())
        assertTrue(atom.getConsumables(Spin.UP).isEmpty())
        assertTrue(atom.getConsumables(Spin.DOWN).isEmpty())
        assertNotNull(atom.hash)
        assertNotNull(atom.hid)
        assertEquals(0L, atom.timestamp)
        assertNotNull(atom.toString())

        assertEquals(atom, Atom(null))
    }
}
