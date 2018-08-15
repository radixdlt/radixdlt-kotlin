package com.radixdlt.client.core.atoms

import com.google.gson.JsonObject

class UnknownAtom(obj: JsonObject) : Atom() {
    private val representation: JsonObject = obj.deepCopy()
}