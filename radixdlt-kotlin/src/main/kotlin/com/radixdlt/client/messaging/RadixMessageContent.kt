package com.radixdlt.client.messaging

import com.google.gson.*
import com.radixdlt.client.core.address.RadixAddress

class RadixMessageContent(val to: RadixAddress?, val from: RadixAddress?, val content: String) {

    fun toJson(): String {
        return GSON.toJson(this)
    }

    fun createReply(replyContent: String): RadixMessageContent {
        return RadixMessageContent(from, to, replyContent)
    }

    override fun toString(): String {
        return "$from -> $to: $content"
    }

    companion object {
        private val ADDRESS_DESERIALIZER = JsonDeserializer<RadixAddress> { json, _, _ -> RadixAddress(json.asString) }
        private val ADDRESS_SERIALIZER = JsonSerializer<RadixAddress> { src, _, _ -> JsonPrimitive(src.toString()) }

        private val GSON = GsonBuilder()
                .registerTypeAdapter(RadixAddress::class.java, ADDRESS_DESERIALIZER)
                .registerTypeAdapter(RadixAddress::class.java, ADDRESS_SERIALIZER)
                .create()

        fun fromJson(json: String): RadixMessageContent {
            return GSON.fromJson(json, RadixMessageContent::class.java)
        }
    }
}
