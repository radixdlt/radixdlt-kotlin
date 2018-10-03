package com.radixdlt.client.application

import com.nhaarman.mockitokotlin2.anyOrNull
import com.radixdlt.client.application.RadixApplicationAPI.Result
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.application.objects.UnencryptedData
import com.radixdlt.client.application.translate.DataStoreTranslator
import com.radixdlt.client.application.translate.InsufficientFundsException
import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.AtomObservation
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.CryptoException
import com.radixdlt.client.core.ledger.AtomPuller
import com.radixdlt.client.core.ledger.AtomStore
import com.radixdlt.client.core.ledger.AtomSubmitter
import com.radixdlt.client.core.ledger.ParticleStore
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import com.radixdlt.client.util.any
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class RadixApplicationAPITest {
    private fun createMockedAPI(
        atomSubmitter: AtomSubmitter,
        atomStore: AtomStore
    ): RadixApplicationAPI {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixUniverse.Ledger::class.java)
        `when`(ledger.getAtomSubmitter()).thenReturn(atomSubmitter)
        `when`(ledger.getAtomStore()).thenReturn(atomStore)

        `when`(ledger.getParticleStore()).thenReturn(object : ParticleStore {
            override fun getConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
                return Observable.just(emptySet())
            }
        })

        `when`(universe.ledger).thenReturn(ledger)
        val identity = mock(RadixIdentity::class.java)

        val atomBuilder = mock(AtomBuilder::class.java)
        `when`(atomBuilder.type(anyOrNull<Class<Atom>>())).thenReturn(atomBuilder)
        `when`(atomBuilder.protectors(anyOrNull())).thenReturn(atomBuilder)
        `when`(atomBuilder.payload(anyOrNull<ByteArray>())).thenReturn(atomBuilder)
        val atom = mock(Atom::class.java)
        `when`(identity.sign(anyOrNull())).thenReturn(Single.just(atom))

        val atomBuilderSupplier = { atomBuilder }
        val unsignedAtom = mock(UnsignedAtom::class.java)
        `when`(atomBuilder.buildWithPOWFee(anyInt(), anyOrNull())).thenReturn(unsignedAtom)

        return RadixApplicationAPI.create(identity, universe, DataStoreTranslator.instance, atomBuilderSupplier)
    }

    private fun createMockedSubmissionWhichAlwaysSucceeds(): AtomSubmitter {
        val submission = mock(AtomSubmitter::class.java)

        val submitting = mock(AtomSubmissionUpdate::class.java)
        val submitted = mock(AtomSubmissionUpdate::class.java)
        val stored = mock(AtomSubmissionUpdate::class.java)
        `when`(submitting.isComplete).thenCallRealMethod()
        `when`(submitted.isComplete).thenCallRealMethod()
        `when`(stored.isComplete).thenCallRealMethod()

        `when`(submitting.getState()).thenReturn(AtomSubmissionState.SUBMITTING)
        `when`(submitted.getState()).thenReturn(AtomSubmissionState.SUBMITTED)
        `when`(stored.getState()).thenReturn(AtomSubmissionState.STORED)

        `when`(submission.submitAtom(any())).thenReturn(Observable.just(submitting, submitted, stored))

        return submission
    }

    private fun createMockedAPIWhichAlwaysSucceeds(): RadixApplicationAPI {
        return createMockedAPI(createMockedSubmissionWhichAlwaysSucceeds(), object : AtomStore {
            override fun getAtoms(destination: EUID?): Observable<AtomObservation> {
                return Observable.never()
            }
        })
    }

    private fun validateSuccessfulStoreDataResult(result: Result) {
        val completionObserver = TestObserver.create<Any>()
        val updatesObserver = TestObserver.create<AtomSubmissionUpdate>()
        result.toCompletable().subscribe(completionObserver)
        completionObserver.assertNoErrors()
        completionObserver.assertComplete()

        result.toObservable().subscribe(updatesObserver)
        updatesObserver.assertNoErrors()
        updatesObserver.assertComplete()
        updatesObserver.assertValueCount(3)
        updatesObserver.assertValueAt(0) { atomUpdate -> atomUpdate.getState() == AtomSubmissionState.SUBMITTING }
        updatesObserver.assertValueAt(1) { atomUpdate -> atomUpdate.getState() == AtomSubmissionState.SUBMITTED }
        updatesObserver.assertValueAt(2) { atomUpdate -> atomUpdate.getState() == AtomSubmissionState.STORED }
    }

//    @Test
//    fun testNull() {
//        assertThatThrownBy { RadixApplicationAPI.create(null) }
//            .isInstanceOf(NullPointerException::class.java)
//
//        val api = createMockedAPIWhichAlwaysSucceeds()
//        assertThatThrownBy { api.getReadableData(null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { api.getTokenTransfers(null, null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { api.getBalance(null, null) }
//            .isInstanceOf(NullPointerException::class.java)
//    }

    @Test
    fun testStoreData() {
        val api = createMockedAPIWhichAlwaysSucceeds()
        val address = mock(RadixAddress::class.java)

        val encryptedData = mock(Data::class.java)
        val result = api.storeData(encryptedData, address)
        validateSuccessfulStoreDataResult(result)
    }

    @Test
    fun testStoreData2() {
        val api = createMockedAPIWhichAlwaysSucceeds()
        val address = mock(RadixAddress::class.java)

        val encryptedData = mock(Data::class.java)
        val result = api.storeData(encryptedData, address, address)
        validateSuccessfulStoreDataResult(result)
    }

    @Test
    fun testStoreWithoutSubscription() {
        val submitter = createMockedSubmissionWhichAlwaysSucceeds()
        val api = createMockedAPI(submitter, object : AtomStore {
            override fun getAtoms(destination: EUID?): Observable<AtomObservation> {
                return Observable.never()
            }
        })
        val address = mock(RadixAddress::class.java)

        val encryptedData = mock(Data::class.java)
        api.storeData(encryptedData, address, address)
        verify(submitter, times(1)).submitAtom(any())
    }

    @Test
    fun testStoreWithMultipleSubscribes() {
        val submitter = createMockedSubmissionWhichAlwaysSucceeds()
        val api = createMockedAPI(submitter, object : AtomStore {
            override fun getAtoms(destination: EUID?): Observable<AtomObservation> {
                return Observable.never()
            }
        })
        val address = mock(RadixAddress::class.java)

        val encryptedData = mock(Data::class.java)
        val result = api.storeData(encryptedData, address, address)
        val observable = result.toObservable()
        observable.subscribe()
        observable.subscribe()
        observable.subscribe()
        verify(submitter, times(1)).submitAtom(any())
    }

    @Test
    fun testUndecryptableData() {
        val identity = mock(RadixIdentity::class.java)
        val universe = mock(RadixUniverse::class.java)
        val address = mock(RadixAddress::class.java)
        val unencryptedData = mock(UnencryptedData::class.java)

        `when`(identity.decrypt(any()))
            .thenReturn(Single.error(CryptoException("Can't decrypt")))
            .thenReturn(Single.just(unencryptedData))

        val data = mock(Data::class.java)
        val dataStoreTranslator = mock(DataStoreTranslator::class.java)
        `when`(dataStoreTranslator.fromAtom(any())).thenReturn(data, data)

        val errorAtom = mock(ApplicationPayloadAtom::class.java)
        `when`(errorAtom.isMessageAtom).thenReturn(true)
        `when`(errorAtom.asMessageAtom).thenReturn(errorAtom)
        val okAtom = mock(ApplicationPayloadAtom::class.java)
        `when`(okAtom.isMessageAtom).thenReturn(true)
        `when`(okAtom.asMessageAtom).thenReturn(okAtom)

        val ledger = mock(RadixUniverse.Ledger::class.java)
        `when`(ledger.getAtomStore()).thenReturn(object : AtomStore {
            override fun getAtoms(destination: EUID?): Observable<AtomObservation> {
                return Observable.just(errorAtom, okAtom).map(AtomObservation.Companion::storeAtom)
            }
        })

        // Extra in Kotlin so that test passes due to null parameter check in kotlin
        `when`(ledger.getParticleStore()).thenReturn(object : ParticleStore {
            override fun getConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
                return Observable.just(emptySet())
            }
        })

        `when`(universe.ledger).thenReturn(ledger)

        val api = RadixApplicationAPI.create(identity, universe, dataStoreTranslator, ::AtomBuilder)
        val observer = TestObserver.create<Any>()
        api.getReadableData(address).subscribe(observer)

        observer.assertValueCount(1)
        observer.assertNoErrors()
    }

    @Test
    fun testZeroTransactionWallet() {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixUniverse.Ledger::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        `when`(ledger.getAtomStore()).thenReturn(object : AtomStore {
            override fun getAtoms(destination: EUID?): Observable<AtomObservation> {
                return Observable.empty()
            }
        })
        `when`(ledger.getParticleStore()).thenReturn(object : ParticleStore {
            override fun getConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
                return Observable.just(emptySet())
            }
        })

        val address = mock(RadixAddress::class.java)
        val identity = mock(RadixIdentity::class.java)

        val api = RadixApplicationAPI.create(identity, universe, DataStoreTranslator.instance, ::AtomBuilder)
        val observer = TestObserver.create<Amount>()

        api.getBalance(address, Asset.TEST).subscribe(observer)
        observer.assertValue { amount -> amount.amountInSubunits == 0L }
    }

    @Test
    fun createTransactionWithNoFunds() {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixUniverse.Ledger::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        `when`(ledger.getAtomStore()).thenReturn(object : AtomStore {
            override fun getAtoms(destination: EUID?): Observable<AtomObservation> {
                return Observable.empty()
            }
        })
        `when`(ledger.getParticleStore()).thenReturn(object : ParticleStore {
            override fun getConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
                return Observable.just(emptySet())
            }
        })
        `when`(ledger.getAtomSubmitter()).thenReturn(object : AtomSubmitter {
            override fun submitAtom(atom: Atom): Observable<AtomSubmissionUpdate> {
                return Observable.empty()
            }
        })

        val address = mock(RadixAddress::class.java)
        `when`(universe.getAddressFrom(any())).thenReturn(address)
        val identity = mock(RadixIdentity::class.java)

        val api = RadixApplicationAPI.create(identity, universe, DataStoreTranslator.instance, ::AtomBuilder)

        val observer = TestObserver.create<Any>()
        api.transferTokens(address, address, Amount.subUnitsOf(10, Asset.TEST)).toCompletable().subscribe(observer)
        observer.assertError(InsufficientFundsException(Asset.TEST, 0, 10))
    }

    @Test
    fun testPullOnReadDataOfOtherAddresses() {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixUniverse.Ledger::class.java)
        val puller = mock(AtomPuller::class.java)
        `when`(ledger.getAtomPuller()).thenReturn(puller)
        val atomStore = mock(AtomStore::class.java)
        `when`(atomStore.getAtoms(anyOrNull())).thenReturn(Observable.never())

        `when`(universe.ledger).thenReturn(ledger)

        `when`(ledger.getAtomStore()).thenReturn(object : AtomStore {
            override fun getAtoms(destination: EUID?): Observable<AtomObservation> {
                return Observable.empty()
            }
        })

        `when`(ledger.getParticleStore()).thenReturn(object : ParticleStore {
            override fun getConsumables(address: RadixAddress): Observable<Collection<Consumable>> {
                return Observable.just(emptySet())
            }
        })

        val identity = mock(RadixIdentity::class.java)
        val address = mock(RadixAddress::class.java)
        val euid = mock(EUID::class.java)
        `when`(address.getUID()).thenReturn(euid)

        val api = RadixApplicationAPI.create(identity, universe, DataStoreTranslator.instance) { AtomBuilder() }
        val testObserver = TestObserver.create<Data>()
        api.getData(address).subscribe(testObserver)
        verify(puller, times(1)).pull(euid)
    }

    @Test
    fun testPullOnGetBalanceOfOtherAddresses() {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixUniverse.Ledger::class.java)
        val puller = mock(AtomPuller::class.java)
        `when`(ledger.getAtomPuller()).thenReturn(puller)
        val particleStore = mock(ParticleStore::class.java)

        `when`(particleStore.getConsumables(anyOrNull())).thenReturn(Observable.never())
        `when`(ledger.getParticleStore()).thenReturn(particleStore)
        `when`(universe.ledger).thenReturn(ledger)

        val identity = mock(RadixIdentity::class.java)
        val address = mock(RadixAddress::class.java)
        val euid = mock(EUID::class.java)
        `when`(address.getUID()).thenReturn(euid)

        val api = RadixApplicationAPI.create(identity, universe, DataStoreTranslator.instance) { AtomBuilder() }
        val testObserver = TestObserver.create<Amount>()
        api.getBalance(address, Asset.TEST).subscribe(testObserver)
        verify(puller, times(1)).pull(euid)
    }
}
