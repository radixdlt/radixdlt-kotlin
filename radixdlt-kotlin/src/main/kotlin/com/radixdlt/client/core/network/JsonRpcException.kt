package com.radixdlt.client.core.network

import com.google.gson.JsonObject

class JsonRpcException(request: JsonObject, error: JsonObject) :
        Exception("Error: " + error.toString() + " on request: " + request.toString())