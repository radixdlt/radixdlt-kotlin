package com.radixdlt.client.messaging

import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.EncryptedPayload
import com.radixdlt.client.core.crypto.CryptoException
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.ledger.RadixLedger
import com.radixdlt.client.util.any
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class RadixMessagingTest {

    @Test
    fun testReceiveUndecryptableMessage() {
        val key = mock(ECPublicKey::class.java)
        val myIdentity = mock(RadixIdentity::class.java)
        `when`(myIdentity.getPublicKey()).thenReturn(key)
        val uid = mock(EUID::class.java)
        `when`(myIdentity.getPublicKey().getUID()).thenReturn(uid)
        val observer = TestObserver.create<RadixMessage>()

        val undecryptableAtom = mock(ApplicationPayloadAtom::class.java)
        `when`(undecryptableAtom.applicationId).thenReturn(RadixMessaging.APPLICATION_ID)

        val decryptableAtom = mock(ApplicationPayloadAtom::class.java)
        `when`(decryptableAtom.applicationId).thenReturn(RadixMessaging.APPLICATION_ID)

        `when`(myIdentity.decrypt(any()))
                .thenReturn(Single.error(CryptoException("Can't decrypt")))
                .thenReturn(Single.just(RadixMessageContent(null, null, "Hello").toJson().toByteArray()))

        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixLedger::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        `when`<Observable<Atom>>(ledger.getAllAtoms(any(), any())).thenReturn(Observable.just(undecryptableAtom, decryptableAtom))

        val messaging = RadixMessaging(universe)
        messaging.getAllMessagesDecrypted(myIdentity)
                .subscribe(observer)

        observer.assertValueCount(1)
        observer.assertNoErrors()
    }

    @Test
    fun testBadMessage() {
        val key = mock(ECPublicKey::class.java)
        val myIdentity = mock(RadixIdentity::class.java)
        `when`(myIdentity.getPublicKey()).thenReturn(key)
        val uid = mock(EUID::class.java)
        `when`(myIdentity.getPublicKey().getUID()).thenReturn(uid)
        val observer = TestObserver.create<RadixMessage>()

        val undecryptableAtom = mock(ApplicationPayloadAtom::class.java)
        `when`(undecryptableAtom.applicationId).thenReturn(RadixMessaging.APPLICATION_ID)

        val decryptableAtom = mock(ApplicationPayloadAtom::class.java)
        `when`(decryptableAtom.applicationId).thenReturn(RadixMessaging.APPLICATION_ID)

        `when`(myIdentity.decrypt(any()))
                .thenReturn(Single.just(byteArrayOf(0, 1, 2, 3)))
                .thenReturn(Single.just(RadixMessageContent(null, null, "Hello").toJson().toByteArray()))

        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixLedger::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        `when`<Observable<Atom>>(ledger.getAllAtoms(any(), any())).thenReturn(Observable.just(undecryptableAtom, decryptableAtom))

        val messaging = RadixMessaging(universe)
        messaging.getAllMessagesDecrypted(myIdentity)
                .subscribe(observer)

        observer.assertValueCount(1)
        observer.assertNoErrors()
    }
}
