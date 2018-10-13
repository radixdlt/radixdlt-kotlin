package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.mock

class InMemoryAtomStoreTest {

    @Test
    fun subscribeBeforeStoreAtomTest() {
        val inMemoryAtomStore = InMemoryAtomStore()
        val atom = mock(Atom::class.java)
        val address = mock(RadixAddress::class.java)

        val testObserver = TestObserver.create<Atom>()
        inMemoryAtomStore.getAtoms(address).subscribe(testObserver)
        inMemoryAtomStore.store(address, atom)

        testObserver.assertValue(atom)
    }

    @Test
    fun subscribeAfterStoreAtomTest() {
        val inMemoryAtomStore = InMemoryAtomStore()
        val atom = mock(Atom::class.java)
        val address = mock(RadixAddress::class.java)

        val testObserver = TestObserver.create<Atom>()
        inMemoryAtomStore.store(address, atom)
        inMemoryAtomStore.getAtoms(address).subscribe(testObserver)

        testObserver.assertValue(atom)
    }
}
