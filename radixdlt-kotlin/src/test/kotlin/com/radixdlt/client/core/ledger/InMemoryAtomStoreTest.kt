package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.mock

class InMemoryAtomStoreTest {

    @Test
    fun subscribeBeforeStoreAtomTest() {
        val inMemoryAtomStore = InMemoryAtomStore()
        val atom = mock(Atom::class.java)

        val testObserver = TestObserver.create<Atom>()
        inMemoryAtomStore.getAtoms(EUID(1)).subscribe(testObserver)
        inMemoryAtomStore.store(EUID(1), atom)

        testObserver.assertValue(atom)
    }

    @Test
    fun subscribeAfterStoreAtomTest() {
        val inMemoryAtomStore = InMemoryAtomStore()
        val atom = mock(Atom::class.java)

        val testObserver = TestObserver.create<Atom>()
        inMemoryAtomStore.store(EUID(1), atom)
        inMemoryAtomStore.getAtoms(EUID(1)).subscribe(testObserver)

        testObserver.assertValue(atom)
    }
}
