package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.objects.Token
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections

class TokenTransferTranslatorTest {

    @Test
    fun testSendToSelfTest() {
        val universe = mock<RadixUniverse>()
        val particleStore = mock<ParticleStore>()
        val atom = mock<Atom>()
        val myKey = mock<ECPublicKey>()
        val myAddress = mock<RadixAddress>()
        whenever(universe.getAddressFrom(myKey)).thenReturn(myAddress)
        whenever(atom.tokenSummary()).thenReturn(
            Collections.singletonMap<EUID, Map<ECPublicKey, Long>>(
                Token.TEST.id,
                Collections.singletonMap(myKey, 0L)
            )
        )

        val tokenTransferTranslator = TokenTransferTranslator(universe, particleStore)
        val tokenTransfers = tokenTransferTranslator.fromAtom(atom)
        assertEquals(myAddress, tokenTransfers[0].from)
        assertEquals(myAddress, tokenTransfers[0].to)
    }

    @Test
    fun createTransactionWithNoFunds() {
        val universe = mock<RadixUniverse>()
        val address = mock<RadixAddress>()

        val transferTranslator = TokenTransferTranslator(universe, object : ParticleStore {
            override fun getConsumables(address: RadixAddress): Observable<Consumable> {
                return Observable.never()
            }
        })
        val tokenTransfer = mock<TokenTransfer>()
        whenever(tokenTransfer.subUnitAmount).thenReturn(10L)
        whenever(tokenTransfer.from).thenReturn(address)
        whenever(tokenTransfer.token).thenReturn(Token.TEST)

        val observer = TestObserver.create<Any>()
        transferTranslator.translate(tokenTransfer, AtomBuilder()).subscribe(observer)
        observer.awaitTerminalEvent()
        observer.assertError(InsufficientFundsException(Token.TEST, 0, 10))
    }
}
