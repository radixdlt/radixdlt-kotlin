package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.UniqueProperty
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.IdParticle
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

        val ecPublicKey = uniqueProperty.address.publicKey
        val particle = IdParticle.create("test", EUID(uniqueProperty.unique), ecPublicKey)
        atomBuilder.addParticle(particle)
        return Completable.complete()
    }
}
