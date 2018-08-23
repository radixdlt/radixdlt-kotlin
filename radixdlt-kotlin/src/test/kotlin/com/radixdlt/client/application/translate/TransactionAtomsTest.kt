package com.radixdlt.client.application.translate

import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.Consumer
import com.radixdlt.client.core.atoms.PayloadAtom
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.util.any
import com.radixdlt.client.util.eq
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class TransactionAtomsTest {

    @Test
    fun testConsumerWithNoConsumable() {
        val keyPair = ECKeyPair(ECPublicKey(ByteArray(33)))
        val address = mock(RadixAddress::class.java)
        `when`(address.ownsKey(any(ECKeyPair::class.java))).thenReturn(true)
        `when`(address.ownsKey(any(ECPublicKey::class.java))).thenReturn(true)

        /* Build atom with consumer originating from nowhere */
        val unsignedAtom = AtomBuilder()
                .type(PayloadAtom::class.java)
                .addParticle(Consumer(100, keyPair, 1, Asset.XRD.id))
                .addParticle(Consumable(100, keyPair, 2, Asset.XRD.id))
                .build()

        val observer = TestObserver.create<Collection<Consumable>>()

        /* Make sure we don't count it unless we find the matching consumable */
        val transactionAtoms = TransactionAtoms(address, Asset.XRD.id)
        transactionAtoms.accept(unsignedAtom.rawAtom.asTransactionAtom)
                .getUnconsumedConsumables().subscribe(observer)
        observer.assertValueCount(0)
    }

    @Test
    fun testConsumerBeforeConsumable() {
        val publicKey = ECPublicKey(ByteArray(33))
        val keyPair = ECKeyPair(publicKey)

        val otherRaw = ByteArray(33)
        otherRaw[0] = 1
        val otherPublicKey = ECPublicKey(otherRaw)
        val otherKeyPair = ECKeyPair(otherPublicKey)

        val address = mock(RadixAddress::class.java)
        `when`(address.ownsKey(eq(keyPair))).thenReturn(true)
        `when`(address.ownsKey(eq(otherKeyPair))).thenReturn(false)
        `when`(address.ownsKey(eq(publicKey))).thenReturn(true)
        `when`(address.ownsKey(eq(otherPublicKey))).thenReturn(false)

        /* Atom with consumer originating from nowhere */
        val unsignedAtom = AtomBuilder()
                .type(PayloadAtom::class.java)
                .addParticle(Consumer(100, keyPair, 1, Asset.XRD.id))
                .addParticle(Consumable(100, keyPair, 2, Asset.XRD.id))
                .build()

        /* Atom with consumable for previous atom's consumer */
        val unsignedAtom2 = AtomBuilder()
                .type(PayloadAtom::class.java)
                .addParticle(Consumer(100, otherKeyPair, 1, Asset.XRD.id))
                .addParticle(Consumable(100, keyPair, 1, Asset.XRD.id))
                .build()

        val observer = TestObserver.create<Collection<Consumable>>()

        /* Make sure we don't count it unless we find the matching consumable */
        val transactionAtoms = TransactionAtoms(address, Asset.XRD.id)
        transactionAtoms.accept(unsignedAtom.rawAtom.asTransactionAtom)
        transactionAtoms.accept(unsignedAtom2.rawAtom.asTransactionAtom)
                .getUnconsumedConsumables()
                .subscribe(observer)

        observer.assertValue { collection -> collection.stream().findFirst().get().nonce == 2L }
    }
}
