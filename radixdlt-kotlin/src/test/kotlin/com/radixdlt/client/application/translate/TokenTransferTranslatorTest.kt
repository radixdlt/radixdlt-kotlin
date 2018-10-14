package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.ledger.ParticleStore
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class TokenTransferTranslatorTest {

    @Test
    fun testSendToSelfTest() {
        val universe = mock<RadixUniverse>()
        val particleStore = mock<ParticleStore>()
        val atom = mock<Atom>()
        val myKey = mock<ECPublicKey>()
        val myAddress = mock<RadixAddress>()
        whenever(universe.getAddressFrom(myKey)).thenReturn(myAddress)
        val tokenReference = mock<TokenRef>()
        whenever(atom.tokenSummary()).thenReturn(mapOf(tokenReference to mapOf(myKey to 0L)))

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
            override fun getParticles(address: RadixAddress): Observable<Particle> {
                return Observable.never()
            }
        })
        val tokenTransfer = mock<TokenTransfer>()
        whenever(tokenTransfer.amount).thenReturn(BigDecimal("1.0"))
        whenever(tokenTransfer.from).thenReturn(address)
        val tokenReference = mock<TokenRef>()
        whenever(tokenTransfer.tokenRef).thenReturn(tokenReference)

        val observer = TestObserver.create<Any>()
        transferTranslator.translate(tokenTransfer, AtomBuilder()).subscribe(observer)
        observer.awaitTerminalEvent()
        observer.assertError(InsufficientFundsException(tokenReference, BigDecimal.ZERO, BigDecimal("1.0")))
    }
}
