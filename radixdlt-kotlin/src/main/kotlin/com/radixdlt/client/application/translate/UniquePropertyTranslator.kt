package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.UniqueProperty
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.atoms.UniqueParticle
import io.reactivex.Completable
import java.util.Objects

/**
 * Translates an application layer unique property object to an atom level object;
 */
class UniquePropertyTranslator {
    fun translate(uniqueProperty: UniqueProperty?, atomBuilder: AtomBuilder): Completable {
        Objects.requireNonNull(atomBuilder)

        if (uniqueProperty == null) {
            return Completable.complete()
        }

        val payload = Payload(uniqueProperty.unique)
        val ecPublicKey = uniqueProperty.address.publicKey
        val uniqueParticle = UniqueParticle.create(payload, ecPublicKey)
        atomBuilder.setUniqueParticle(uniqueParticle)
        return Completable.complete()
    }
}
