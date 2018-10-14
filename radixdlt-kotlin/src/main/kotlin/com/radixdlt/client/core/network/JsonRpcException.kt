package com.radixdlt.client.core.network

import com.google.gson.JsonObject

class JsonRpcException(val request: JsonObject, val error: JsonObject) :
    Exception(error.getAsJsonObject("error")?.getAsJsonPrimitive("message")?.asString ?: error.toString())

