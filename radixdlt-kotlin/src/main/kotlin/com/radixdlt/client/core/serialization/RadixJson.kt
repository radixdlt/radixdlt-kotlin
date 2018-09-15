package com.radixdlt.client.core.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixUniverseType
import com.radixdlt.client.core.atoms.AbstractConsumable
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomFeeConsumable
import com.radixdlt.client.core.atoms.ChronoParticle
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.Consumer
import com.radixdlt.client.core.atoms.DataParticle
import com.radixdlt.client.core.atoms.Emission
import com.radixdlt.client.core.atoms.Particle
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.atoms.UniqueParticle
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.network.NodeRunnerData
import com.radixdlt.client.core.util.Base64Encoded
import com.radixdlt.client.core.util.Int128
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import java.io.IOException
import java.lang.reflect.Type
import java.util.HashMap

object RadixJson {

    private val BASE64_SERIALIZER =
        JsonSerializer<Base64Encoded> { src, _, _ -> serializedValue("BASE64", src.base64()) }

    private val PAYLOAD_DESERIALIZER =
        JsonDeserializer<Payload> { json, _, _ -> Payload.fromBase64(json.asJsonObject.get("value").asString) }

    private val PK_DESERIALIZER = JsonDeserializer<ECPublicKey> { json, _, _ ->
        val publicKey = Base64.decode(json.asJsonObject.get("value").asString)
        ECPublicKey(publicKey)
    }

    private val PROTECTOR_DESERIALIZER = JsonDeserializer<EncryptedPrivateKey> { json, _, _ ->
        val encryptedPrivateKey = Base64.decode(json.asJsonObject.get("value").asString)
        EncryptedPrivateKey(encryptedPrivateKey)
    }

    private val UNIVERSER_TYPE_DESERIALIZER =
        JsonDeserializer<RadixUniverseType> { json, _, _ -> RadixUniverseType.valueOf(json.asInt) }

    private val NODE_RUNNDER_DATA_JSON_DESERIALIZER = JsonDeserializer<NodeRunnerData> { json, _, _ ->
        NodeRunnerData(
            if (json.asJsonObject.has("host")) json.asJsonObject.get("host").asJsonObject.get("ip").asString else null,
            json.asJsonObject.get("system").asJsonObject.get("shards").asJsonObject.get("low").asLong,
            json.asJsonObject.get("system").asJsonObject.get("shards").asJsonObject.get("high").asLong
        )
    }

    private val ABSTRACT_CONSUMABLE_SERIALIZER = JsonSerializer<Particle> { particle, _, context ->
        when {
            particle.javaClass == AtomFeeConsumable::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", -1463653224)
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            particle.javaClass == Consumable::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", 318720611)
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            particle.javaClass == Emission::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", 1782261127)
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            else -> throw RuntimeException("Unknown Particle: " + particle.javaClass)
        }
    }

    private val ABSTRACT_CONSUMABLE_DESERIALIZER = JsonDeserializer<Particle> { json, _, context ->
        val serializer = json.asJsonObject.get("serializer").asLong
        return@JsonDeserializer when (serializer) {
            -1463653224L -> context.deserialize(json.asJsonObject, AtomFeeConsumable::class.java)
            318720611L -> context.deserialize(json.asJsonObject, Consumable::class.java)
            1782261127L -> context.deserialize(json.asJsonObject, Emission::class.java)
            else -> throw RuntimeException("Unknown particle serializer: $serializer")
        }
    }

    private val SERIALIZERS = HashMap<Class<*>, Int>()

    init {
        SERIALIZERS[Atom::class.java] = 2019665
        SERIALIZERS[ECKeyPair::class.java] = 547221307
        SERIALIZERS[ECSignature::class.java] = -434788200
        SERIALIZERS[DataParticle::class.java] = 473758768
        SERIALIZERS[UniqueParticle::class.java] = "UNIQUEPARTICLE".hashCode()
        SERIALIZERS[ChronoParticle::class.java] = "CHRONOPARTICLE".hashCode()
        SERIALIZERS[Consumer::class.java] = 214856694
    }

    private val ECKEYPAIR_ADAPTER_FACTORY = object : TypeAdapterFactory {
        override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
            val serializer = SERIALIZERS[type.rawType] ?: return null
            val delegate = gson.getDelegateAdapter(this, type)
            val elementAdapter = gson.getAdapter(JsonElement::class.java)

            return object : TypeAdapter<T>() {
                @Throws(IOException::class)
                override fun write(out: JsonWriter, value: T) {
                    val tree = delegate.toJsonTree(value)
                    if (!tree.isJsonNull) {
                        tree.asJsonObject.addProperty("serializer", serializer)
                        tree.asJsonObject.addProperty("version", 100)
                    }
                    elementAdapter.write(out, tree)
                }

                @Throws(IOException::class)
                override fun read(`in`: JsonReader): T {
                    val tree = elementAdapter.read(`in`)
                    return delegate.fromJsonTree(tree)
                }
            }
        }
    }

    @JvmStatic
    val gson: Gson

    init {
        val gsonBuilder = GsonBuilder()
            .registerTypeHierarchyAdapter(Base64Encoded::class.java, BASE64_SERIALIZER)
            .registerTypeAdapterFactory(ECKEYPAIR_ADAPTER_FACTORY)
            .registerTypeAdapter(ByteArray::class.java, ByteArraySerializer())
            .registerTypeAdapter(AbstractConsumable::class.java, ABSTRACT_CONSUMABLE_SERIALIZER)
            .registerTypeAdapter(AbstractConsumable::class.java, ABSTRACT_CONSUMABLE_DESERIALIZER)
            .registerTypeAdapter(EUID::class.java, EUIDSerializer())
            .registerTypeAdapter(Payload::class.java, PAYLOAD_DESERIALIZER)
            .registerTypeAdapter(EncryptedPrivateKey::class.java, PROTECTOR_DESERIALIZER)
            .registerTypeAdapter(ECPublicKey::class.java, PK_DESERIALIZER)
            .registerTypeAdapter(RadixUniverseType::class.java, UNIVERSER_TYPE_DESERIALIZER)
            .registerTypeAdapter(NodeRunnerData::class.java, NODE_RUNNDER_DATA_JSON_DESERIALIZER)

        gson = gsonBuilder.create()
    }

    private fun serializedValue(type: String, value: String): JsonObject {
        val element = JsonObject()
        element.addProperty("serializer", type)
        element.addProperty("value", value)
        return element
    }

    private class EUIDSerializer : JsonDeserializer<EUID>, JsonSerializer<EUID> {
        override fun serialize(src: EUID, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return serializedValue("EUID", src.toString())
        }

        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EUID {
            return EUID(Int128.from(Hex.decode(json.asJsonObject.get("value").asString)))
        }
    }

    private class ByteArraySerializer : JsonDeserializer<ByteArray>, JsonSerializer<ByteArray> {
        override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return serializedValue("BASE64", Base64.toBase64String(src))
        }

        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ByteArray {
            return Base64.decode(json.asJsonObject.get("value").asString)
        }
    }
}
