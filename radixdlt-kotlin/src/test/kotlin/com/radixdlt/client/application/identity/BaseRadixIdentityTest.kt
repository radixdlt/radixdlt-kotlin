package com.radixdlt.client.application.identity

import com.nhaarman.mockitokotlin2.anyOrNull
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.UnsignedAtom
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECSignature
import io.reactivex.observers.TestObserver
import org.junit.Test

import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class BaseRadixIdentityTest {

    @Test
    fun signTest() {
        val keyPair = mock(ECKeyPair::class.java)
        val ecSignature = mock(ECSignature::class.java)
        val euid = mock(EUID::class.java)
        `when`(keyPair.sign(anyOrNull())).thenReturn(ecSignature)
        `when`(keyPair.getUID()).thenReturn(euid)

        val signedAtom = mock(Atom::class.java)
        `when`(signedAtom.getSignature(anyOrNull())).thenReturn(ecSignature)
        val hash = mock(RadixHash::class.java)
        val atom = mock(UnsignedAtom::class.java)
        `when`(atom.sign(anyOrNull(), anyOrNull())).thenReturn(signedAtom)
        `when`(atom.hash).thenReturn(hash)
        val identity = BaseRadixIdentity(keyPair)
        val testObserver = TestObserver.create<Atom>()
        identity.sign(atom).subscribe(testObserver)

        verify(keyPair, never()).getPrivateKey()

        testObserver.assertValue { a -> a.getSignature(euid)!! == ecSignature }
    }
}
