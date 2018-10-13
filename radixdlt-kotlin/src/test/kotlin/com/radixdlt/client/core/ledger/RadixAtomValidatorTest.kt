package com.radixdlt.client.core.ledger

import com.radixdlt.client.application.objects.Token
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomValidationException
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.util.any
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.Arrays

class RadixAtomValidatorTest {

    @Test
    fun testSignatureValidation() {
        val hash = mock(RadixHash::class.java)

        val keyPair = mock(ECKeyPair::class.java)
        val publicKey = mock(ECPublicKey::class.java)
        `when`(keyPair.getUID()).thenReturn(EUID(1))
        `when`(keyPair.getPublicKey()).thenReturn(publicKey)
        `when`(publicKey.getUID()).thenReturn(EUID(1))

        val consumer = mock(Consumable::class.java)
        `when`(consumer.getOwnersPublicKeys()).thenReturn(setOf(publicKey))
        `when`(consumer.getTokenClass()).thenReturn(Token.TEST.id)

        val atom = mock(Atom::class.java)
        `when`(atom.hash).thenReturn(hash)
        `when`(atom.getSignature(any())).thenReturn(null)
        `when`(atom.getConsumables(Spin.DOWN)).thenReturn(Arrays.asList(consumer))

        val validator = RadixAtomValidator.getInstance()
        assertThatThrownBy { validator.validateSignatures(atom) }
            .isInstanceOf(AtomValidationException::class.java)
    }

    @Test
    @Throws(AtomValidationException::class)
    fun testPayloadValidationWithNoSignatures() {
        val hash = mock(RadixHash::class.java)

        val keyPair = mock(ECKeyPair::class.java)
        val publicKey = mock(ECPublicKey::class.java)
        `when`(keyPair.getUID()).thenReturn(EUID(1))
        `when`(keyPair.getPublicKey()).thenReturn(publicKey)
        `when`(publicKey.getUID()).thenReturn(EUID(1))

        val atom = mock(Atom::class.java)
        `when`(atom.hash).thenReturn(hash)
        `when`(atom.getSignature(any())).thenReturn(null)

        val validator = RadixAtomValidator.getInstance()
        validator.validateSignatures(atom)
    }
}
