package com.radixdlt.client.core.network

import com.google.gson.JsonObject
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.serialization.SerializedAtomType

class AtomQuery<T : Atom>(val destination: EUID, val atomClass: Class<T>) {
    private val atomType: SerializedAtomType?

    init {

        if (atomClass == Atom::class.java) {
            this.atomType = null
        } else {
            val atomType = SerializedAtomType.valueOf(atomClass)
            if (atomType == null) {
                throw IllegalArgumentException("Cannot serialize atom class: $atomClass")
            }
            this.atomType = atomType
        }
    }

    fun getAtomType(): SerializedAtomType? {
        return atomType
    }

    fun toJson(): JsonObject {
        val query = JsonObject()
        query.addProperty("destination", destination.bigInteger())

        if (atomType != null) {
            query.addProperty("atomSerializer", atomType.serializer)
        }

        return query
    }
}
