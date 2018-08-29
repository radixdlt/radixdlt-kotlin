package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import java.util.Collections

/**
 * Particle which manages an Encryptor or a private key encrypted for specified
 * readers.
 */
class EncryptorParticle(protectors: List<EncryptedPrivateKey>) {
    val protectors: List<EncryptedPrivateKey> = Collections.unmodifiableList(protectors)
}
