package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigInteger

class InMemoryAtomStoreTest {

    @Test
    fun subscribeBeforeStoreAtomTest() {
        val inMemoryAtomStore = InMemoryAtomStore()
        val atom = mock(Atom::class.java)

        val testObserver = TestObserver.create<Atom>()
        inMemoryAtomStore.getAtoms(EUID(BigInteger.ONE)).subscribe(testObserver)
        inMemoryAtomStore.store(EUID(BigInteger.ONE), atom)

        testObserver.assertValue(atom)
    }

    @Test
    fun subscribeAfterStoreAtomTest() {
        val inMemoryAtomStore = InMemoryAtomStore()
        val atom = mock(Atom::class.java)

        val testObserver = TestObserver.create<Atom>()
        inMemoryAtomStore.store(EUID(BigInteger.ONE), atom)
        inMemoryAtomStore.getAtoms(EUID(BigInteger.ONE)).subscribe(testObserver)

        testObserver.assertValue(atom)
    }
}
