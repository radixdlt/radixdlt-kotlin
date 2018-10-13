package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AbstractConsumable
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Test

class TokenTransferTranslatorTest {

    @Test
    fun createTransactionWithNoFunds() {
        val universe = mock<RadixUniverse>()
        val address = mock<RadixAddress>()

        val transferTranslator = TokenTransferTranslator(universe, object : ParticleStore {
            override fun getConsumables(address: RadixAddress): Observable<AbstractConsumable> {
                return Observable.never()
            }
        })
        val tokenTransfer = mock<TokenTransfer>()
        whenever(tokenTransfer.subUnitAmount).thenReturn(10L)
        whenever(tokenTransfer.from).thenReturn(address)
        whenever(tokenTransfer.tokenClass).thenReturn(Asset.TEST)

        val observer = TestObserver.create<Any>()
        transferTranslator.translate(tokenTransfer, AtomBuilder()).subscribe(observer)
        observer.awaitTerminalEvent()
        observer.assertError(InsufficientFundsException(Asset.TEST, 0, 10))
    }
}
