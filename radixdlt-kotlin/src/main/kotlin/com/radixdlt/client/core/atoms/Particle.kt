package com.radixdlt.client.core.atoms

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.serialization.Dson

abstract class Particle {
    val destinations: Set<EUID>?
    val owners: Set<ECKeyPair>?

    val ownersPublicKeys: Set<ECPublicKey>
        get() =
            if (owners == null) emptySet() else owners.asSequence().map { it.getPublicKey() }.toSet()

    val isAbstractConsumable: Boolean
        get() = this is AbstractConsumable

    val isConsumable: Boolean
        get() = this is Consumable

    val isConsumer: Boolean
        get() = this is Consumer

    val asConsumer: Consumer
        get() = this as Consumer

    val asConsumable: Consumable
        get() = this as Consumable

    val asAbstractConsumable: AbstractConsumable
        get() = this as AbstractConsumable

    val hash: RadixHash
        get() = RadixHash.of(dson)

    val dson: ByteArray
        get() = Dson.instance.toDson(this)

    constructor() {
        this.destinations = null
        this.owners = null
    }

    constructor(destinations: Set<EUID>) {
        this.destinations = destinations
        this.owners = null
    }

    constructor(destinations: Set<EUID>?, owners: Set<ECKeyPair>) {
        this.destinations = destinations
        this.owners = owners
    }

    override fun toString(): String {
        return "${this.javaClass.name} owners($owners)"
    }
}
