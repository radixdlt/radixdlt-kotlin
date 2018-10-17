package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.application.actions.TransferTokensAction
import com.radixdlt.client.application.objects.TokenTransfer
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AbstractConsumable
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.TransactionAtom
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Test
import java.util.Collections

class TransferTokensActionTranslatorTest {

    @Test
    fun testSendToSelfTest() {
        val universe = mock<RadixUniverse>()
        val particleStore = mock<ParticleStore>()
        val atom = mock<TransactionAtom>()
        val myKey = mock<ECPublicKey>()
        val myAddress = mock<RadixAddress>()
        whenever(universe.getAddressFrom(myKey)).thenReturn(myAddress)
        whenever(atom.summary()).thenReturn(
            Collections.singletonMap(
                setOf(myKey), Collections.singletonMap(Asset.TEST.id, 0L)
            )
        )

        val testObserver = TestObserver.create<TokenTransfer>()
        val tokenTransferTranslator = TokenTransferTranslator(universe, particleStore)
        tokenTransferTranslator.fromAtom(atom, mock()).subscribe(testObserver)
        testObserver.assertValue { transfer -> myAddress == transfer.from && myAddress == transfer.to }
    }

    @Test
    fun createTransactionWithNoFunds() {
        val universe = mock<RadixUniverse>()
        val address = mock<RadixAddress>()

        val transferTranslator = TokenTransferTranslator(universe, object : ParticleStore {
            override fun getConsumables(address: RadixAddress): Observable<AbstractConsumable> {
                return Observable.never()
            }
        })
        val transferTokensAction = mock<TransferTokensAction>()
        whenever(transferTokensAction.subUnitAmount).thenReturn(10L)
        whenever(transferTokensAction.from).thenReturn(address)
        whenever(transferTokensAction.tokenClass).thenReturn(Asset.TEST)

        val observer = TestObserver.create<Any>()
        transferTranslator.translate(transferTokensAction, AtomBuilder()).subscribe(observer)
        observer.awaitTerminalEvent()
        observer.assertError(InsufficientFundsException(Asset.TEST, 0, 10))
    }
}
