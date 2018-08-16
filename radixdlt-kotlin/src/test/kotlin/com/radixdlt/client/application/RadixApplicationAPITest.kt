package com.radixdlt.client.application


import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.crypto.CryptoException
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.crypto.Encryptor
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.ledger.RadixLedger
import com.radixdlt.client.util.any
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class RadixApplicationAPITest {

    @Test
    fun testUndecryptableData() {
        val identity = mock(RadixIdentity::class.java)
        val ledger = mock(RadixLedger::class.java)
        val address = mock(RadixAddress::class.java)

        `when`(identity.decrypt(any()))
                .thenReturn(Single.error(CryptoException("Can't decrypt")))
                .thenReturn(Single.just(byteArrayOf(0)))

        val encryptor = mock(Encryptor::class.java)
        val protector = mock(EncryptedPrivateKey::class.java)
        `when`(encryptor.protectors).thenReturn(listOf(protector))

        val payload = mock(Payload::class.java)

        val errorAtom = mock(ApplicationPayloadAtom::class.java)
        `when`<Encryptor>(errorAtom.encryptor).thenReturn(encryptor)
        `when`(errorAtom.encrypted).thenReturn(payload)

        val okAtom = mock(ApplicationPayloadAtom::class.java)
        `when`<Encryptor>(okAtom.encryptor).thenReturn(encryptor)
        `when`(okAtom.encrypted).thenReturn(payload)

        `when`<Observable<Atom>>(ledger.getAllAtoms(any(), any())).thenReturn(Observable.just<Atom>(errorAtom, okAtom))

        val api = RadixApplicationAPI.create(identity, ledger)
        val observer = TestObserver.create<Any>()
        api.getDecryptableData(address).subscribe(observer)

        observer.assertValueCount(1)
        observer.assertNoErrors()
    }
}
