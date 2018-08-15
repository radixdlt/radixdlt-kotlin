package com.radixdlt.client.wallet

import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.TransactionAtom
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.util.any
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.*

class WalletTransactionTest {
    @Test
    fun getAmountTest() {
        val address = mock(RadixAddress::class.java)
        val atom = mock(TransactionAtom::class.java)
        val ecKeyPair = mock(ECKeyPair::class.java)
        val publicKey = mock(ECPublicKey::class.java)
        `when`(ecKeyPair.getPublicKey()).thenReturn(publicKey)
        `when`(address.ownsKey(any(ECKeyPair::class.java))).thenReturn(true)
        `when`(address.ownsKey(any(ECPublicKey::class.java))).thenReturn(true)
        val consumable1 = Consumable(10, ecKeyPair, 0, Asset.XRD.id)
        val consumable2 = Consumable(10, ecKeyPair, 1, Asset.XRD.id)
        `when`(atom.particles).thenReturn(Arrays.asList(consumable1, consumable2))

        val transaction = WalletTransaction(address, atom)
        assertEquals(20, transaction.amount)
    }
}