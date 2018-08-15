package com.radixdlt.client.core.serialization

import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.NullAtom
import com.radixdlt.client.core.atoms.TransactionAtom

enum class SerializedAtomType(val atomClass: Class<out Atom>, val serializer: Long) {
    TRANSACTION(TransactionAtom::class.java, -760130L),
    NULL(NullAtom::class.java, -1123323048L),
    MESSAGE(ApplicationPayloadAtom::class.java, -2040291185L);


    companion object {

        @JvmStatic
        fun valueOf(atomClass: Class<out Atom>): SerializedAtomType? {
            for (atomType in SerializedAtomType.values()) {
                if (atomType.atomClass == atomClass) {
                    return atomType
                }
            }

            return null
        }

        @JvmStatic
        fun valueOf(serializer: Long): SerializedAtomType? {
            for (atomType in SerializedAtomType.values()) {
                if (atomType.serializer == serializer) {
                    return atomType
                }
            }

            return null
        }
    }
}
