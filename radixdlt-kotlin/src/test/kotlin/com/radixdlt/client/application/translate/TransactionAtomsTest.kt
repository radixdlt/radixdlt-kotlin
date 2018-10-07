package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import io.reactivex.observers.TestObserver
import org.junit.Test

class TransactionAtomsTest {

    @Test
    fun testConsumerWithNoConsumable() {
        val accountReference = mock<AccountReference>()
        val ecPublicKey = mock<ECPublicKey>()
        whenever(accountReference.getKey()).thenReturn(ecPublicKey)

        val address = mock<RadixAddress>()
        whenever(address.ownsKey(any<ECKeyPair>())).thenReturn(true)
        whenever(address.ownsKey(any<ECPublicKey>())).thenReturn(true)

        val consumer = mock<Consumable>()
        whenever(consumer.tokenClass).thenReturn(Asset.TEST.id)
        whenever(consumer.ownersPublicKeys).thenReturn(setOf(ecPublicKey))
        whenever(consumer.dson).thenReturn(byteArrayOf(0))

        val consumable = mock<Consumable>()
        whenever(consumable.tokenClass).thenReturn(Asset.TEST.id)
        whenever(consumable.ownersPublicKeys).thenReturn(setOf(ecPublicKey))
        whenever(consumable.dson).thenReturn(byteArrayOf(1))

        // Build atom with consumer originating from nowhere
        val atom = mock<Atom>()
        whenever(atom.getConsumers()).thenReturn(listOf(consumer))
        whenever(atom.getConsumables()).thenReturn(listOf(consumable))

        // Make sure we don't count it unless we find the matching consumable
        val transactionAtoms = TransactionAtoms(address, Asset.TEST.id)

        val observer = TestObserver.create<Collection<Consumable>>()
        transactionAtoms.accept(atom).getUnconsumedConsumables().subscribe(observer)
        observer.assertValueCount(0)
    }

    @Test
    fun testConsumerBeforeConsumable() {
        val accountReference = mock<AccountReference>()
        val ecPublicKey = mock<ECPublicKey>()
        whenever(accountReference.getKey()).thenReturn(ecPublicKey)

        val address = mock<RadixAddress>()
        whenever(address.ownsKey(ecPublicKey)).thenReturn(true)

        val consumer = mock<Consumable>()
        whenever(consumer.tokenClass).thenReturn(Asset.TEST.id)
        whenever(consumer.ownersPublicKeys).thenReturn(setOf(ecPublicKey))
        whenever(consumer.dson).thenReturn(byteArrayOf(0))

        val consumable = mock<Consumable>()
        whenever(consumable.tokenClass).thenReturn(Asset.TEST.id)
        whenever(consumable.ownersPublicKeys).thenReturn(setOf(ecPublicKey))
        whenever(consumable.dson).thenReturn(byteArrayOf(1))

        val atom = mock<Atom>()
        whenever(atom.getConsumers()).thenReturn(listOf(consumer))
        whenever(atom.getConsumables()).thenReturn(listOf(consumable))

        val oldConsumable = mock<Consumable>()
        whenever(oldConsumable.tokenClass).thenReturn(Asset.TEST.id)
        whenever(oldConsumable.ownersPublicKeys).thenReturn(setOf(ecPublicKey))
        whenever(oldConsumable.dson).thenReturn(byteArrayOf(0))

        val oldConsumer = mock<Consumable>()
        whenever(oldConsumer.tokenClass).thenReturn(Asset.TEST.id)
        whenever(oldConsumer.ownersPublicKeys).thenReturn(
            setOf(mock())
        )
        whenever(oldConsumer.dson).thenReturn(byteArrayOf(2))

        val oldAtom = mock<Atom>()
        whenever(oldAtom.getConsumers()).thenReturn(listOf(oldConsumer))
        whenever(oldAtom.getConsumables()).thenReturn(listOf(oldConsumable))

        val observer = TestObserver.create<Collection<Consumable>>()

        /* Make sure we don't count it unless we find the matching consumable */
        val transactionAtoms = TransactionAtoms(address, Asset.TEST.id)
        transactionAtoms.accept(atom)
        transactionAtoms.accept(oldAtom)
            .getUnconsumedConsumables()
            .subscribe(observer)

        observer.assertValue { collection -> collection.stream().findFirst().get().dson[0].toInt() == 1 }
    }
}
