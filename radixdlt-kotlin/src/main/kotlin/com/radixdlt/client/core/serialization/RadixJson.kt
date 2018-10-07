package com.radixdlt.client.core.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.radixdlt.client.core.TokenClassReference
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixUniverseType
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.AssetParticle
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomFeeConsumable
import com.radixdlt.client.core.atoms.ChronoParticle
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.DataParticle
import com.radixdlt.client.core.atoms.Emission
import com.radixdlt.client.core.atoms.MetadataMap
import com.radixdlt.client.core.atoms.Particle
import com.radixdlt.client.core.atoms.Payload
import com.radixdlt.client.core.crypto.ECKeyPair
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.crypto.EncryptedPrivateKey
import com.radixdlt.client.core.network.NodeRunnerData
import com.radixdlt.client.core.serialization.SerializationConstants.BYT_PREFIX
import com.radixdlt.client.core.serialization.SerializationConstants.STR_PREFIX
import com.radixdlt.client.core.serialization.SerializationConstants.UID_PREFIX
import com.radixdlt.client.core.util.Base64Encoded
import com.radixdlt.client.core.util.Int128
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import java.io.IOException
import java.lang.reflect.Type
import java.util.HashMap

object RadixJson {

    private fun checkPrefix(value: String, prefix: String): String {
        if (!value.startsWith(prefix)) {
            throw IllegalStateException("JSON value does not start with prefix $prefix")
        }
        return value.substring(prefix.length)
    }

    private fun unString(value: String): String {
        return if (value.startsWith(STR_PREFIX)) value.substring(STR_PREFIX.length) else value
    }

    private val BASE64_SERIALIZER =
        JsonSerializer<Base64Encoded> { src, _, _ -> JsonPrimitive(BYT_PREFIX + src.base64()) }

    private val PAYLOAD_DESERIALIZER =
        JsonDeserializer<Payload> { json, _, _ -> Payload.fromBase64(checkPrefix(json.asString, BYT_PREFIX)) }

    private val PK_DESERIALIZER = JsonDeserializer<ECPublicKey> { json, _, _ ->
        val publicKey = Base64.decode(checkPrefix(json.asString, BYT_PREFIX))
        ECPublicKey(publicKey)
    }

    private val PROTECTOR_DESERIALIZER = JsonDeserializer<EncryptedPrivateKey> { json, _, _ ->
        val encryptedPrivateKey = Base64.decode(checkPrefix(json.asString, BYT_PREFIX))
        EncryptedPrivateKey(encryptedPrivateKey)
    }

    private val UNIVERSE_TYPE_DESERIALIZER =
        JsonDeserializer<RadixUniverseType> { json, _, _ -> RadixUniverseType.valueOf(json.asInt) }

    private val NODE_RUNNER_DATA_JSON_DESERIALIZER = JsonDeserializer<NodeRunnerData> { json, _, _ ->
        val obj = json.asJsonObject
        NodeRunnerData(
            if (obj.has("host")) unString(obj.get("host").asJsonObject.get("ip").asString) else null,
            obj.get("system").asJsonObject.get("shards").asJsonObject.get("low").asLong,
            obj.get("system").asJsonObject.get("shards").asJsonObject.get("high").asLong
        )
    }

    private val PARTICLE_SERIALIZER_IDS = HashMap<Class<out Particle>, Long>()
    init {
        PARTICLE_SERIALIZER_IDS[AtomFeeConsumable::class.java] = "FEEPARTICLE".hashCode().toLong()
        PARTICLE_SERIALIZER_IDS[Consumable::class.java] = "TRANSFERPARTICLE".hashCode().toLong()
        PARTICLE_SERIALIZER_IDS.put(Emission::class.java, 1341978856L)
        PARTICLE_SERIALIZER_IDS.put(DataParticle::class.java, 473758768L)
        // PARTICLE_SERIALIZER_IDS.put(UniqueParticle.class, Long.valueOf("UNIQUEPARTICLE".hashCode()));
        PARTICLE_SERIALIZER_IDS[ChronoParticle::class.java] = "CHRONOPARTICLE".hashCode().toLong()
        PARTICLE_SERIALIZER_IDS.put(AssetParticle::class.java, - 1034420571L)
    }

    private val PARTICLE_SERIALIZER = JsonSerializer<Particle> { particle, _, context ->
        val id = PARTICLE_SERIALIZER_IDS[particle.javaClass]
        if (id != null) {
            val jsonParticle = context.serialize(particle).asJsonObject
            jsonParticle.addProperty("serializer", id)
            jsonParticle.addProperty("version", 100)
            return@JsonSerializer jsonParticle
        }

        throw RuntimeException("Unknown Particle: " + particle.javaClass)
    }

    private val PARTICLE_DESERIALIZER = JsonDeserializer<Particle> { json, _, context ->
        val serializer = json.asJsonObject.get("serializer").asLong
        val c = PARTICLE_SERIALIZER_IDS.entries.asSequence().filter { e -> e.value == serializer }
            .map { it.key }
            .firstOrNull()
        if (c != null) {
            return@JsonDeserializer context.deserialize(json.asJsonObject, c as Class<*>)
        }

        throw RuntimeException("Unknown particle serializer: $serializer")
    }

    private val SERIALIZERS = HashMap<Class<*>, Int>()

    init {
        SERIALIZERS[Atom::class.java] = 2019665
        SERIALIZERS[ECKeyPair::class.java] = 547221307
        SERIALIZERS[ECSignature::class.java] = -434788200
        SERIALIZERS[TokenClassReference::class.java] = "TOKENCLASSREFERENCE".hashCode()
        SERIALIZERS[AccountReference::class.java] = "ACCOUNTREFERENCE".hashCode()
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
            .registerTypeAdapter(Particle::class.java, PARTICLE_SERIALIZER)
            .registerTypeAdapter(Particle::class.java, PARTICLE_DESERIALIZER)
            .registerTypeAdapter(String::class.java, StringCodec())
            .registerTypeAdapter(MetadataMap::class.java, MetadataCodec())
            .registerTypeAdapter(EUID::class.java, EUIDSerializer())
            .registerTypeAdapter(Payload::class.java, PAYLOAD_DESERIALIZER)
            .registerTypeAdapter(EncryptedPrivateKey::class.java, PROTECTOR_DESERIALIZER)
            .registerTypeAdapter(ECPublicKey::class.java, PK_DESERIALIZER)
            .registerTypeAdapter(RadixUniverseType::class.java, UNIVERSE_TYPE_DESERIALIZER)
            .registerTypeAdapter(NodeRunnerData::class.java, NODE_RUNNER_DATA_JSON_DESERIALIZER)

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
            return JsonPrimitive(UID_PREFIX + src.toString())
        }

        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EUID {
            return EUID(Int128.from(Hex.decode(checkPrefix(json.asString, UID_PREFIX))));
        }
    }

    private class ByteArraySerializer : JsonDeserializer<ByteArray>, JsonSerializer<ByteArray> {
        override fun serialize(src: ByteArray, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(BYT_PREFIX + Base64.toBase64String(src))
        }

        @Throws(JsonParseException::class)
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ByteArray {
            return Base64.decode(checkPrefix(json.asString, BYT_PREFIX));
        }
    }

    private class StringCodec : JsonDeserializer<String>, JsonSerializer<String> {
        override fun serialize(src: String, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(STR_PREFIX + src)
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): String {
            return unString(json.asString)
        }
    }

    private class MetadataCodec : JsonDeserializer<MetadataMap>, JsonSerializer<MetadataMap> {
        override fun serialize(src: MetadataMap, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val obj = JsonObject()
            for ((key, value) in src) {
                obj.addProperty(key, STR_PREFIX + value)
            }
            return obj
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MetadataMap {
            val obj = json as JsonObject
            val map = MetadataMap()
            for ((key, value) in obj.entrySet()) {
                map[key] = unString(value.asString)
            }
            return map
        }
    }
}
