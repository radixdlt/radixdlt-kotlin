package com.radixdlt.client.application

import com.radixdlt.client.application.RadixApplicationAPI.Result
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.application.objects.UnencryptedData
import com.radixdlt.client.application.translate.InsufficientFundsException
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.atoms.TransactionAtom
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.CryptoException
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.crypto.Encryptor
import com.radixdlt.client.core.ledger.RadixLedger
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
    private fun createMockedAPI(ledger: RadixLedger): RadixApplicationAPI {
        val universe = mock(RadixUniverse::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        val identity = mock(RadixIdentity::class.java)

        val atomBuilder = mock(AtomBuilder::class.java)
        `when`(atomBuilder.type(any<Class<Atom>>())).thenReturn(atomBuilder)
        `when`(atomBuilder.protectors(any())).thenReturn(atomBuilder)
        `when`(atomBuilder.payload(any(ByteArray::class.java))).thenReturn(atomBuilder)
        val atom = mock(Atom::class.java)
        `when`(identity.sign(any())).thenReturn(Single.just(atom))

        val atomBuilderSupplier = { atomBuilder }
        val unsignedAtom = mock(UnsignedAtom::class.java)
        `when`(atomBuilder.buildWithPOWFee(anyInt(), any())).thenReturn(unsignedAtom)

        return RadixApplicationAPI.create(identity, universe, atomBuilderSupplier)
    }

    private fun createMockedLedgerWhichAlwaysSucceeds(): RadixLedger {
        val ledger = mock(RadixLedger::class.java)

        val submitting = mock(AtomSubmissionUpdate::class.java)
        val submitted = mock(AtomSubmissionUpdate::class.java)
        val stored = mock(AtomSubmissionUpdate::class.java)
        `when`(submitting.isComplete).thenCallRealMethod()
        `when`(submitted.isComplete).thenCallRealMethod()
        `when`(stored.isComplete).thenCallRealMethod()

        `when`(submitting.getState()).thenReturn(AtomSubmissionState.SUBMITTING)
        `when`(submitted.getState()).thenReturn(AtomSubmissionState.SUBMITTED)
        `when`(stored.getState()).thenReturn(AtomSubmissionState.STORED)

        `when`(ledger.submitAtom(any())).thenReturn(Observable.just(submitting, submitted, stored))

        return ledger
    }

    private fun createMockedAPIWhichAlwaysSucceeds(): RadixApplicationAPI {
        return createMockedAPI(createMockedLedgerWhichAlwaysSucceeds())
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
        val ledger = createMockedLedgerWhichAlwaysSucceeds()
        val api = createMockedAPI(ledger)
        val address = mock(RadixAddress::class.java)

        val encryptedData = mock(Data::class.java)
        api.storeData(encryptedData, address, address)
        verify(ledger, times(1)).submitAtom(any())
    }

    @Test
    fun testStoreWithMultipleSubscribes() {
        val ledger = createMockedLedgerWhichAlwaysSucceeds()
        val api = createMockedAPI(ledger)
        val address = mock(RadixAddress::class.java)

        val encryptedData = mock(Data::class.java)
        val result = api.storeData(encryptedData, address, address)
        val observable = result.toObservable()
        observable.subscribe()
        observable.subscribe()
        observable.subscribe()
        verify(ledger, times(1)).submitAtom(any())
    }

    @Test
    fun testUndecryptableData() {
        val identity = mock(RadixIdentity::class.java)
        val ledger = mock(RadixLedger::class.java)
        val universe = mock(RadixUniverse::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        val address = mock(RadixAddress::class.java)
        val unencryptedData = mock(UnencryptedData::class.java)

        `when`(identity.decrypt(any()))
            .thenReturn(Single.error(CryptoException("Can't decrypt")))
            .thenReturn(Single.just(unencryptedData))

        val encryptor = mock(Encryptor::class.java)
        val protector = mock(EncryptedPrivateKey::class.java)
        `when`(encryptor.protectors).thenReturn(listOf(protector))

        val payload = mock(Payload::class.java)

        val errorAtom = mock(TransactionAtom::class.java)
        `when`(errorAtom.encryptor).thenReturn(encryptor)
        `when`(errorAtom.encrypted).thenReturn(payload)

        val okAtom = mock(TransactionAtom::class.java)
        `when`(okAtom.encryptor).thenReturn(encryptor)
        `when`(okAtom.encrypted).thenReturn(payload)

        `when`(ledger.getAllAtoms(any(), any<Class<Atom>>())).thenReturn(Observable.just(errorAtom, okAtom))

        val api = RadixApplicationAPI.create(identity, universe, ::AtomBuilder)
        val observer = TestObserver.create<Any>()
        api.getReadableData(address).subscribe(observer)

        observer.assertValueCount(1)
        observer.assertNoErrors()
    }

    @Test
    fun testZeroTransactionWallet() {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixLedger::class.java)
        val address = mock(RadixAddress::class.java)
        val identity = mock(RadixIdentity::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        val api = RadixApplicationAPI.create(identity, universe, ::AtomBuilder)

        `when`(ledger.getAllAtoms(any(), any<Class<Atom>>())).thenReturn(Observable.empty())

        val observer = TestObserver.create<Long>()

        api.getSubUnitBalance(address, Asset.XRD).subscribe(observer)
        observer.assertValue(0L)
    }

    @Test
    fun createTransactionWithNoFunds() {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixLedger::class.java)
        val address = mock(RadixAddress::class.java)
        val identity = mock(RadixIdentity::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        val api = RadixApplicationAPI.create(identity, universe, ::AtomBuilder)

        `when`(ledger.getAllAtoms(any(), any<Class<Atom>>())).thenReturn(Observable.empty())

        val observer = TestObserver.create<Any>()
        api.transferTokens(address, address, Asset.XRD, 10).toCompletable().subscribe(observer)
        observer.assertError(InsufficientFundsException(Asset.XRD, 0, 10))
    }
}
