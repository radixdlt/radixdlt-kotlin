package com.radixdlt.client.core.ledger

import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomValidationException
import com.radixdlt.client.core.atoms.Consumer
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.util.any
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.Arrays

class RadixAtomValidatorTest {

    @Test(expected = AtomValidationException::class)
    @Throws(AtomValidationException::class)
    fun testSignatureValidation() {
        val hash = mock(RadixHash::class.java)

        val keyPair = mock(ECKeyPair::class.java)
        val publicKey = mock(ECPublicKey::class.java)
        `when`(keyPair.getUID()).thenReturn(EUID(1))
        `when`(keyPair.getPublicKey()).thenReturn(publicKey)
        `when`(publicKey.getUID()).thenReturn(EUID(1))

        val consumer = mock(Consumer::class.java)
        `when`(consumer.isAbstractConsumable).thenReturn(true)
        `when`(consumer.asAbstractConsumable).thenReturn(consumer)
        `when`(consumer.ownersPublicKeys).thenReturn(setOf(publicKey))
        `when`(consumer.assetId).thenReturn(Asset.TEST.id)

        val atom = mock(Atom::class.java)
        `when`(atom.hash).thenReturn(hash)
        `when`(atom.getSignature(any())).thenReturn(null)
        `when`(atom.particles).thenReturn(Arrays.asList(consumer))

        val validator = RadixAtomValidator.getInstance()
        validator.validateSignatures(atom)
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

        val consumer = mock(Consumer::class.java)
        `when`(consumer.isAbstractConsumable).thenReturn(true)
        `when`(consumer.asAbstractConsumable).thenReturn(consumer)
        `when`(consumer.ownersPublicKeys).thenReturn(setOf(publicKey))
        `when`(consumer.assetId).thenReturn(Asset.TEST.id)

        val atom = mock(Atom::class.java)
        `when`(atom.hash).thenReturn(hash)
        `when`(atom.getSignature(any())).thenReturn(null)

        val validator = RadixAtomValidator.getInstance()
        validator.validateSignatures(atom)
    }
}
