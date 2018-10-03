package com.radixdlt.client.core.atoms

class AtomObservation private constructor(val atom: Atom?, private val type: Type, private val receivedTimestamp: Long) {

    val isStore: Boolean
        get() = type == Type.STORE

    val isHead: Boolean
        get() = type == Type.HEAD

    enum class Type {
        STORE,
        HEAD
    }

    companion object {

        @JvmStatic
        fun storeAtom(atom: Atom): AtomObservation {
            return AtomObservation(atom, Type.STORE, System.currentTimeMillis())
        }

        @JvmStatic
        fun head(): AtomObservation {
            return AtomObservation(null, Type.HEAD, System.currentTimeMillis())
        }
    }
}
