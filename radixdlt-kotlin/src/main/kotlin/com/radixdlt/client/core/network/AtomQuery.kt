package com.radixdlt.client.core.network

import com.google.gson.JsonObject
import com.radixdlt.client.core.address.EUID

class AtomQuery(private val destination: EUID) {

    fun toJson(): JsonObject {
        val query = JsonObject()
        query.addProperty("destination", destination.toString())

        return query
    }
}
