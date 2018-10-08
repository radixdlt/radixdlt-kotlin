package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.TransactionAtom
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.ledger.ParticleStore
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Collections

class TokenTransferTranslatorTest {
    @Test
    fun testSendToSelfTest() {
        val universe = mock<RadixUniverse>()
        val particleStore = mock<ParticleStore>()
        val atom = mock<TransactionAtom>()
        val myKey = mock<ECPublicKey>()
        val myAddress = mock<RadixAddress>()
        whenever(universe.getAddressFrom(myKey)).thenReturn(myAddress)
        whenever(atom.summary()).thenReturn(
            Collections.singletonMap<Set<ECPublicKey>, Map<EUID, Long>>(
                setOf(myKey), Collections.singletonMap<EUID, Long>(Asset.TEST.id, 0L)
            )
        )

        val tokenTransferTranslator = TokenTransferTranslator(universe, particleStore)
        val tokenTransfer = tokenTransferTranslator.fromAtom(atom)
        assertEquals(myAddress, tokenTransfer.from)
        assertEquals(myAddress, tokenTransfer.to)
    }
}
