package com.radixdlt.client.application

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
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
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.CryptoException
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.Collections

class RadixApplicationAPITest {
    private fun createMockedAPI(
        atomSubmission: (Atom) -> (Observable<AtomSubmissionUpdate>),
        atomStore: (EUID) -> (Observable<Atom>)
    ): RadixApplicationAPI {
        val universe = mock(RadixUniverse::class.java)
        `when`(universe.getAtomSubmissionHandler()).thenReturn(atomSubmission)

//        val consumableDataSource = mock(ConsumableDataSource::class.java)
//        `when`(universe.getParticleStore()).thenReturn(consumableDataSource::getConsumables)
//        `when`(universe.getParticleStore()).thenReturn { Observable.just(Collections.emptySet()) }

        `when`(universe.getAtomStore()).thenReturn(atomStore)
        val identity = mock(RadixIdentity::class.java)

        val atomBuilder = mock(AtomBuilder::class.java)
        `when`(atomBuilder.type(any<Class<Atom>>())).thenReturn(atomBuilder)
        `when`(atomBuilder.protectors(any())).thenReturn(atomBuilder)
        `when`(atomBuilder.payload(any<ByteArray>())).thenReturn(atomBuilder)
        val atom = mock(Atom::class.java)
        `when`(identity.sign(any())).thenReturn(Single.just(atom))

        val atomBuilderSupplier = { atomBuilder }
        val unsignedAtom = mock(UnsignedAtom::class.java)
        `when`(atomBuilder.buildWithPOWFee(anyInt(), any())).thenReturn(unsignedAtom)

        return RadixApplicationAPI.create(identity, universe, DataStoreTranslator.instance, atomBuilderSupplier)
    }

    private fun createMockedSubmissionWhichAlwaysSucceeds(): (Atom) -> (Observable<AtomSubmissionUpdate>) {
        val submission = mock<(Atom) -> (Observable<AtomSubmissionUpdate>)>()

        val submitting = mock(AtomSubmissionUpdate::class.java)
        val submitted = mock(AtomSubmissionUpdate::class.java)
        val stored = mock(AtomSubmissionUpdate::class.java)
        `when`(submitting.isComplete).thenCallRealMethod()
        `when`(submitted.isComplete).thenCallRealMethod()
        `when`(stored.isComplete).thenCallRealMethod()

        `when`(submitting.getState()).thenReturn(AtomSubmissionState.SUBMITTING)
        `when`(submitted.getState()).thenReturn(AtomSubmissionState.SUBMITTED)
        `when`(stored.getState()).thenReturn(AtomSubmissionState.STORED)

        `when`(submission(any())).thenReturn(Observable.just(submitting, submitted, stored))

        return submission
    }

    private fun createMockedAPIWhichAlwaysSucceeds(): RadixApplicationAPI {
        return createMockedAPI(createMockedSubmissionWhichAlwaysSucceeds(), { euid -> Observable.never() })
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
        val submission = createMockedSubmissionWhichAlwaysSucceeds()
        val api = createMockedAPI(submission,  { euid -> Observable.never<Atom>() })
        val address = mock(RadixAddress::class.java)

        val encryptedData = mock(Data::class.java)
        api.storeData(encryptedData, address, address)
        verify(submission, times(1)).apply(any())
    }

    @Test
    fun testStoreWithMultipleSubscribes() {
        val submission = createMockedSubmissionWhichAlwaysSucceeds()
        val api = createMockedAPI(submission, { euid -> Observable.never<Atom>() })
        val address = mock(RadixAddress::class.java)

        val encryptedData = mock(Data::class.java)
        val result = api.storeData(encryptedData, address, address)
        val observable = result.toObservable()
        observable.subscribe()
        observable.subscribe()
        observable.subscribe()
        verify(submission, times(1)).apply(any())
    }

    @Test
    fun testUndecryptableData() {
        val identity = mock(RadixIdentity::class.java)
        val universe = mock(RadixUniverse::class.java)
        `when`(universe.getParticleStore()).thenReturn { Observable.just(Collections.emptySet()) }
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

        `when`(universe.getAtomStore()).thenReturn { Observable.just(errorAtom, okAtom) }

        val api = RadixApplicationAPI.create(identity, universe, dataStoreTranslator, ::AtomBuilder)
        val observer = TestObserver.create<Any>()
        api.getReadableData(address).subscribe(observer)

        observer.assertValueCount(1)
        observer.assertNoErrors()
    }

    @Test
    fun testZeroTransactionWallet() {
        val universe = mock(RadixUniverse::class.java)
        `when`(universe.getAtomStore()).thenReturn { Observable.empty() }
        `when`(universe.getParticleStore()).thenReturn { Observable.just(Collections.emptySet()) }

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
        `when`(universe.getAtomStore()).thenReturn { Observable.empty() }
        `when`(universe.getParticleStore()).thenReturn { Observable.just(Collections.emptySet()) }
        `when`(universe.getAtomSubmissionHandler()).thenReturn { Observable.empty() }

        val address = mock(RadixAddress::class.java)
        val identity = mock(RadixIdentity::class.java)

        val api = RadixApplicationAPI.create(identity, universe, DataStoreTranslator.instance, ::AtomBuilder)

        val observer = TestObserver.create<Any>()
        api.transferTokens(address, address, Amount.subUnitsOf(10, Asset.TEST)).toCompletable().subscribe(observer)
        observer.assertError(InsufficientFundsException(Asset.TEST, 0, 10))
    }
}
