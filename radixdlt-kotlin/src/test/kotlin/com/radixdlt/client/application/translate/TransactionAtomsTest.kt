package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.application.objects.Token
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.Spin
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
        whenever(consumer.getTokenClass()).thenReturn(Token.TEST.id)
        whenever(consumer.getOwnersPublicKeys()).thenReturn(setOf(ecPublicKey))
        whenever(consumer.getDson()).thenReturn(byteArrayOf(0))

        val consumable = mock<Consumable>()
        whenever(consumable.getTokenClass()).thenReturn(Token.TEST.id)
        whenever(consumable.getOwnersPublicKeys()).thenReturn(setOf(ecPublicKey))
        whenever(consumable.getDson()).thenReturn(byteArrayOf(1))

        // Build atom with consumer originating from nowhere
        val atom = mock<Atom>()
        whenever(atom.getConsumables(Spin.DOWN)).thenReturn(listOf(consumer))
        whenever(atom.getConsumables(Spin.UP)).thenReturn(listOf(consumable))

        // Make sure we don't count it unless we find the matching consumable
        val transactionAtoms = TransactionAtoms(address, Token.TEST.id)

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
        whenever(consumer.getSpin()).thenReturn(Spin.DOWN)
        whenever(consumer.getTokenClass()).thenReturn(Token.TEST.id)
        whenever(consumer.getOwnersPublicKeys()).thenReturn(setOf(ecPublicKey))
        whenever(consumer.getDson()).thenReturn(byteArrayOf(0))

        val consumable = mock<Consumable>()
        whenever(consumer.getSpin()).thenReturn(Spin.UP)
        whenever(consumable.getTokenClass()).thenReturn(Token.TEST.id)
        whenever(consumable.getOwnersPublicKeys()).thenReturn(setOf(ecPublicKey))
        whenever(consumable.getDson()).thenReturn(byteArrayOf(1))

        val atom = mock<Atom>()
        whenever(atom.getConsumables(Spin.DOWN)).thenReturn(listOf(consumer))
        whenever(atom.getConsumables(Spin.UP)).thenReturn(listOf(consumable))

        val oldConsumable = mock<Consumable>()
        whenever(consumer.getSpin()).thenReturn(Spin.UP)
        whenever(oldConsumable.getTokenClass()).thenReturn(Token.TEST.id)
        whenever(oldConsumable.getOwnersPublicKeys()).thenReturn(setOf(ecPublicKey))
        whenever(oldConsumable.getDson()).thenReturn(byteArrayOf(0))

        val oldConsumer = mock<Consumable>()
        whenever(consumer.getSpin()).thenReturn(Spin.DOWN)
        whenever(oldConsumer.getTokenClass()).thenReturn(Token.TEST.id)
        whenever(oldConsumer.getOwnersPublicKeys()).thenReturn(setOf(mock()))
        whenever(oldConsumer.getDson()).thenReturn(byteArrayOf(2))

        val oldAtom = mock<Atom>()
        whenever(oldAtom.getConsumables(Spin.DOWN)).thenReturn(listOf(oldConsumer))
        whenever(oldAtom.getConsumables(Spin.UP)).thenReturn(listOf(oldConsumable))

        val observer = TestObserver.create<Collection<Consumable>>()

        /* Make sure we don't count it unless we find the matching consumable */
        val transactionAtoms = TransactionAtoms(address, Token.TEST.id)
        transactionAtoms.accept(atom)
        transactionAtoms.accept(oldAtom)
            .getUnconsumedConsumables()
            .subscribe(observer)

        observer.assertValue { collection -> collection.stream().findFirst().get().getDson()[0].toInt() == 1 }
    }
}
