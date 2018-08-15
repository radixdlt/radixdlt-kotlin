package com.radixdlt.client.core.serialization

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixUniverseType
import com.radixdlt.client.core.atoms.*
import com.radixdlt.client.core.atoms.NullAtom.JunkParticle
import com.radixdlt.client.core.crypto.*
import com.radixdlt.client.core.network.NodeRunnerData
import com.radixdlt.client.core.util.Base64Encoded
import org.bouncycastle.util.encoders.Base64
import java.io.IOException
import java.lang.reflect.Type
import java.util.*

object RadixJson {

    private val BASE64_SERIALIZER = JsonSerializer<Base64Encoded> { src, _, _ -> serializedValue("BASE64", src.base64()) }

    private val EUID_SERIALIZER = JsonSerializer<EUID> { uid, _, _ -> serializedValue("EUID", uid.bigInteger().toString()) }

    private val PAYLOAD_DESERIALIZER = JsonDeserializer<Payload> { json, _, _ -> Payload.fromBase64(json.asJsonObject.get("value").asString) }

    private val PK_DESERIALIZER = JsonDeserializer<ECPublicKey> { json, _, _ ->
        val publicKey = Base64.decode(json.asJsonObject.get("value").asString)
        ECPublicKey(publicKey)
    }

    private val PROTECTOR_DESERIALIZER = JsonDeserializer<EncryptedPrivateKey> { json, _, _ ->
        val encryptedPrivateKey = Base64.decode(json.asJsonObject.get("value").asString)
        EncryptedPrivateKey(encryptedPrivateKey)
    }

    private val UNIVERSER_TYPE_DESERIALIZER = JsonDeserializer<RadixUniverseType> { json, _, _ -> RadixUniverseType.valueOf(json.asInt) }

    private val NODE_RUNNDER_DATA_JSON_DESERIALIZER = JsonDeserializer<NodeRunnerData> { json, _, _ ->
        NodeRunnerData(
                if (json.asJsonObject.has("host")) json.asJsonObject.get("host").asJsonObject.get("ip").asString else null,
                json.asJsonObject.get("system").asJsonObject.get("shards").asJsonObject.get("low").asLong,
                json.asJsonObject.get("system").asJsonObject.get("shards").asJsonObject.get("high").asLong
        )
    }

    private val ATOM_DESERIALIZER = JsonDeserializer<Atom> { json, _, context ->
        val serializer = json.asJsonObject.get("serializer").asLong
        val atomType = SerializedAtomType.valueOf(serializer)
        return@JsonDeserializer if (atomType != null) {
            context.deserialize(json.asJsonObject, atomType.atomClass)
        } else {
            UnknownAtom(json.asJsonObject)
        }
    }

    private val ATOM_SERIALIZER = JsonSerializer<Atom> { atom, _, context ->
        val atomType = SerializedAtomType.valueOf(atom.javaClass)
        if (atomType != null) {
            val jsonAtom = context.serialize(atom).asJsonObject
            jsonAtom.addProperty("serializer", atomType.serializer)
            jsonAtom.addProperty("version", 100)
            return@JsonSerializer jsonAtom
        } else {
            throw IllegalArgumentException("Cannot serialize an atom with class: " + atom.javaClass)
        }
    }


    private val PARTICLE_SERIALIZER = JsonSerializer<Particle> { particle, _, context ->
        when {
            particle.javaClass == AtomFeeConsumable::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", -1463653224)
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            particle.javaClass == JunkParticle::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", -1123054001)
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            particle.javaClass == Consumable::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", 318720611)
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            particle.javaClass == Consumer::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", 214856694)
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            particle.javaClass == Emission::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", 1782261127)
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            particle.javaClass == IdParticle::class.java -> {
                val jsonParticle = context.serialize(particle).asJsonObject
                jsonParticle.addProperty("serializer", "IDPARTICLE".hashCode())
                jsonParticle.addProperty("version", 100)
                return@JsonSerializer jsonParticle
            }
            else -> throw RuntimeException("Unknown Particle: " + particle.javaClass)
        }
    }

    private val PARTICLE_DESERIALIZER = JsonDeserializer<Particle> { json, _, context ->
        val serializer = json.asJsonObject.get("serializer").asLong
        return@JsonDeserializer when (serializer) {
            -1463653224L -> context.deserialize(json.asJsonObject, AtomFeeConsumable::class.java)
            318720611L -> context.deserialize(json.asJsonObject, Consumable::class.java)
            214856694L -> context.deserialize(json.asJsonObject, Consumer::class.java)
            1782261127L -> context.deserialize(json.asJsonObject, Emission::class.java)
            -1123054001L -> context.deserialize(json.asJsonObject, JunkParticle::class.java)
            "IDPARTICLE".hashCode().toLong() -> context.deserialize(json.asJsonObject, IdParticle::class.java)
            else -> throw RuntimeException("Unknown particle serializer: $serializer")
        }
    }

    private val SERIALIZERS = HashMap<Class<*>, Int>()

    init {
        SERIALIZERS[ECKeyPair::class.java] = 547221307
        SERIALIZERS[ECSignature::class.java] = -434788200
        SERIALIZERS[Encryptor::class.java] = 105401064
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

    @JvmStatic val gson: Gson

    init {
        val gsonBuilder = GsonBuilder()
                .registerTypeHierarchyAdapter(Base64Encoded::class.java, BASE64_SERIALIZER)
                .registerTypeAdapterFactory(ECKEYPAIR_ADAPTER_FACTORY)
                .registerTypeAdapter(ByteArray::class.java, ByteArraySerializer())
                .registerTypeAdapter(Particle::class.java, PARTICLE_SERIALIZER)
                .registerTypeAdapter(Particle::class.java, PARTICLE_DESERIALIZER)
                .registerTypeAdapter(Atom::class.java, ATOM_SERIALIZER)
                .registerTypeAdapter(Atom::class.java, ATOM_DESERIALIZER)
                .registerTypeAdapter(EUID::class.java, EUID_SERIALIZER)
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
