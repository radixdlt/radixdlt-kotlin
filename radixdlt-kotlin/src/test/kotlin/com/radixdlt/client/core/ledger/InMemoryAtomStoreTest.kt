package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AtomObservation
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigInteger

class InMemoryAtomStoreTest {

    @Test
    fun subscribeBeforeStoreAtomTest() {
        val inMemoryAtomStore = InMemoryAtomStore()
        val atomObservation = mock(AtomObservation::class.java)

        val testObserver = TestObserver.create<AtomObservation>()
        inMemoryAtomStore.getAtoms(EUID(BigInteger.ONE)).subscribe(testObserver)
        inMemoryAtomStore.store(EUID(BigInteger.ONE), atomObservation)

        testObserver.assertValue(atomObservation)
    }

    @Test
    fun subscribeAfterStoreAtomTest() {
        val inMemoryAtomStore = InMemoryAtomStore()
        val atomObservation = mock(AtomObservation::class.java)

        val testObserver = TestObserver.create<AtomObservation>()
        inMemoryAtomStore.store(EUID(BigInteger.ONE), atomObservation)
        inMemoryAtomStore.getAtoms(EUID(BigInteger.ONE)).subscribe(testObserver)

        testObserver.assertValue(atomObservation)
    }
}
