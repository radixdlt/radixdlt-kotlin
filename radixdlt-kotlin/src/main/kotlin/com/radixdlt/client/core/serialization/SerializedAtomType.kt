package com.radixdlt.client.core.serialization

import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.PayloadAtom

enum class SerializedAtomType(val atomClass: Class<out Atom>, val serializer: Long) {
    ATOM(PayloadAtom::class.java, -1231693889);

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
