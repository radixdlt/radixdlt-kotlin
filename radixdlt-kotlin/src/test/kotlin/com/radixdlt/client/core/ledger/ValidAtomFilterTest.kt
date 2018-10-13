package com.radixdlt.client.core.ledger

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.serialization.Dson
import io.reactivex.observers.TestObserver
import org.junit.Test

class ValidAtomFilterTest {

    @Test
    fun testDownWithMissingUp() {
        val accountReference = mock<AccountReference>()
        val ecPublicKey = mock<ECPublicKey>()
        whenever(accountReference.getKey()).thenReturn(ecPublicKey)

        val address = mock<RadixAddress>()
        whenever(address.ownsKey(anyOrNull<ECKeyPair>())).thenReturn(true)
        whenever(address.ownsKey(anyOrNull<ECPublicKey>())).thenReturn(true)

        val down = mock<Particle>()
        whenever(down.getAddresses()).thenReturn(setOf(ecPublicKey))

        val dson = mock<Dson>()
        whenever(dson.toDson(down)).thenReturn(byteArrayOf(0))

        val up = mock<Particle>()
        whenever(up.getAddresses()).thenReturn(setOf(ecPublicKey))
        whenever(dson.toDson(up)).thenReturn(byteArrayOf(1))

        // Build atom with consumer originating from nowhere
        val atom = mock<Atom>()
        whenever(atom.particles(Spin.DOWN)).then { listOf(down) }
        whenever(atom.particles(Spin.UP)).then { listOf(up) }

        val validAtomFilter = ValidAtomFilter(address, dson)

        val observer = TestObserver.create<Atom>()
        validAtomFilter.filter(atom).subscribe(observer)
        observer.assertValueCount(0)
    }

    @Test
    fun testDownBeforeUp() {
        val accountReference = mock<AccountReference>()
        val ecPublicKey = mock<ECPublicKey>()
        whenever(accountReference.getKey()).thenReturn(ecPublicKey)

        val address = mock<RadixAddress>()
        whenever(address.ownsKey(ecPublicKey)).thenReturn(true)

        val down0 = mock<Particle>()
        whenever(down0.getSpin()).thenReturn(Spin.DOWN)
        whenever(down0.getAddresses()).thenReturn(setOf(ecPublicKey))

        val dson = mock<Dson>()
        whenever(dson.toDson(down0)).thenReturn(byteArrayOf(0))

        val up1 = mock<Particle>()
        whenever(up1.getSpin()).thenReturn(Spin.UP)
        whenever(up1.getAddresses()).thenReturn(setOf(ecPublicKey))
        whenever(dson.toDson(up1)).thenReturn(byteArrayOf(1))

        val atom = mock<Atom>()
        whenever(atom.particles(Spin.DOWN)).then { listOf(down0) }
        whenever(atom.particles(Spin.UP)).then { listOf(up1) }

        val up0 = mock<Particle>()
        whenever(up0.getSpin()).thenReturn(Spin.UP)
        whenever(up0.getAddresses()).thenReturn(setOf(ecPublicKey))
        whenever(dson.toDson(up0)).thenReturn(byteArrayOf(0))

        val down2 = mock<Particle>()
        whenever(down2.getSpin()).thenReturn(Spin.DOWN)
        whenever(down2.getAddresses()).thenReturn(setOf(mock()))
        whenever(dson.toDson(down2)).thenReturn(byteArrayOf(2))

        val oldAtom = mock<Atom>()
        whenever(oldAtom.particles(Spin.DOWN)).then { listOf(down2) }
        whenever(oldAtom.particles(Spin.UP)).then { listOf(up0) }

        val observer = TestObserver.create<Atom>()

        /* Make sure we don't count it unless we find the matching consumable */
        val validAtomFilter = ValidAtomFilter(address, dson)
        validAtomFilter.filter(atom)
        validAtomFilter.filter(oldAtom).subscribe(observer)
        observer.assertValues(oldAtom, atom)
    }
}
