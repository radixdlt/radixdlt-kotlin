package com.radixdlt.client.core.serialization

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.serialization.SerializationConstants.BYT_PREFIX
import com.radixdlt.client.core.serialization.SerializationConstants.HSH_PREFIX
import com.radixdlt.client.core.serialization.SerializationConstants.STR_PREFIX
import com.radixdlt.client.core.serialization.SerializationConstants.UID_PREFIX
import com.radixdlt.client.core.util.Base64Encoded
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.HashMap

class Dson private constructor() {

    private val versionField = object : DsonField {
        override fun getName(): String = "version"

        override fun getBytes(): ByteArray = toDson(100L)
    }

    private enum class Primitive(val value: Int) {
        NUMBER(0x20),
        EUID(0x21),
        HASH(0x22),
        BYTES(0x40),
        STRING(0x41),
        ARRAY(0x80),
        OBJECT(0x81);
    }

    private fun parse(byteBuffer: ByteBuffer): JsonElement {
        val type = byteBuffer.get().toInt() and 0xFF
        var length = SerializationUtils.decodeInt(byteBuffer)
        val result: JsonElement
        if (type == Primitive.NUMBER.value) {
            result = JsonPrimitive(byteBuffer.long)
        } else if (type == Primitive.STRING.value) {
            val buffer = ByteArray(length)
            byteBuffer.get(buffer)
            result = JsonPrimitive(STR_PREFIX + String(buffer, StandardCharsets.UTF_8))
        } else if (type == Primitive.BYTES.value) {
            val buffer = ByteArray(length)
            byteBuffer.get(buffer)
            result = JsonPrimitive(BYT_PREFIX + Base64.toBase64String(buffer))
        } else if (type == Primitive.OBJECT.value) {
            val jsonObject = JsonObject()

            while (length > 0) {
                val fieldNameLength = byteBuffer.get().toInt() and 0xFF
                val fieldName = ByteArray(fieldNameLength)
                byteBuffer.get(fieldName)
                val start = byteBuffer.position()
                val child = parse(byteBuffer)
                val end = byteBuffer.position()
                val fieldLength = 1 + fieldNameLength + (end - start)
                length -= fieldLength
                jsonObject.add(String(fieldName), child)
            }

            result = jsonObject
        } else if (type == Primitive.ARRAY.value) {
            val jsonArray = JsonArray()
            while (length > 0) {
                val start = byteBuffer.position()
                val child = parse(byteBuffer)
                val end = byteBuffer.position()
                length -= end - start
                jsonArray.add(child)
            }
            result = jsonArray
        } else if (type == Primitive.EUID.value) {
            val buffer = ByteArray(length)
            byteBuffer.get(buffer)
            result = JsonPrimitive(UID_PREFIX + Hex.toHexString(buffer))
        } else if (type == Primitive.HASH.value) {
            val buffer = ByteArray(length)
            byteBuffer.get(buffer)
            result = JsonPrimitive(HSH_PREFIX + Hex.toHexString(buffer))
        } else {
            throw RuntimeException("Unknown type: $type")
        }

        return result
    }

    fun parse(buffer: ByteArray): JsonElement {
        return parse(ByteBuffer.wrap(buffer))
    }

    interface DsonField {
        fun getName(): String
        fun getBytes(): ByteArray
    }

    fun toDson(o: Any?): ByteArray {
        val raw: ByteArray
        val type: Int

        if (o == null) {
            throw IllegalArgumentException("Null sent")
        } else if (o is Collection<*>) {
            val outputStream = ByteArrayOutputStream()
            for (arrayObject in o) {
                try {
                    val arrayObjRaw = toDson(arrayObject)
                    outputStream.write(arrayObjRaw)
                } catch (e: IOException) {
                    throw UncheckedIOException(e)
                }
            }
            raw = outputStream.toByteArray()
            type = Primitive.ARRAY.value
        } else if (o is Long) {
            raw = longToByteArray((o as Long?)!!)
            type = Primitive.NUMBER.value
        } else if (o is Number) {
            throw IllegalStateException("A number must be a long to be serialized in Dson: $o")
        } else if (o is EUID) {
            raw = o.toByteArray()
            type = Primitive.EUID.value
        } else if (o is Base64Encoded) {
            raw = o.toByteArray()
            type = Primitive.BYTES.value
        } else if (o is String) {
            raw = o.toByteArray()
            type = Primitive.STRING.value
        } else if (o is ByteArray) {
            raw = o
            type = Primitive.BYTES.value
        } else if (o is Map<*, *>) {
            val map = o as Map<*, *>?

            if (HashMap::class.java == o.javaClass) {
                throw IllegalStateException("Cannot DSON serialize HashMap. Must be a predictably ordered map.")
            }

            val fieldStream: Sequence<DsonField> = map!!.entries.asSequence().map { e ->
                object : DsonField {
                    override fun getName(): String = e.key.toString()

                    override fun getBytes(): ByteArray = toDson(e.value)
                }
            }

            val rawList: Sequence<DsonField> = fieldStream.sortedBy { it.getName() }
            raw = toByteArray(rawList)
            type = Primitive.OBJECT.value
        } else if (o is HasOrdinalValue) {
            // HACK
            raw = longToByteArray(o.ordinalValue().toLong())
            type = 2
        } else {
            var c: Class<*> = o.javaClass
            val fields = ArrayList<Field>()
            while (c != Any::class.java) {
                fields.addAll(Arrays.asList(*c.declaredFields))
                c = c.superclass
            }

            val fieldStream = fields.asSequence()
                .filter { field -> !field.name.equals("signatures", ignoreCase = true) }
                .filter { field -> !field.name.equals("serialVersionUID", ignoreCase = true) }
                .filter { field -> !field.name.equals("spin", ignoreCase = true) } // TODO: This needs to be added back in
                .filter { field -> !field.name.equals("Companion", ignoreCase = true) } // Kotlin field
                .filter { field -> !Modifier.isTransient(field.modifiers) }
                .filter { field ->
                    try {
                        field.isAccessible = true
                        return@filter field.get(o) != null
                    } catch (e: IllegalAccessException) {
                        throw RuntimeException()
                    }
                }
                .map { field ->
                    dsonFieldFrom(o, field)
                }

            val rawList = fieldStream.asSequence().plus(versionField).sortedBy { it.getName() }
            raw = toByteArray(rawList)
            type = Primitive.OBJECT.value
        }

        val byteBuffer = ByteBuffer.allocate(5 + raw.size)
        byteBuffer.put(type.toByte())
        byteBuffer.putInt(raw.size)
        byteBuffer.put(raw)

        return byteBuffer.array()
    }

    fun toByteArray(rawList: Sequence<DsonField>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        rawList.forEach { dsonField ->
            try {
                val nameBytes = dsonField.getName().toByteArray(StandardCharsets.UTF_8)
                SerializationUtils.encodeInt(nameBytes.size, outputStream)
                outputStream.write(nameBytes)
                outputStream.write(dsonField.getBytes())
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }
        return outputStream.toByteArray()
    }

    private fun dsonFieldFrom(o: Any, field: Field): DsonField {
        val serializedName = field.getAnnotation(SerializedName::class.java)
        val name = serializedName?.value ?: field.name

        return object : DsonField {
            override fun getName(): String = name

            override fun getBytes(): ByteArray {
                try {
                    field.isAccessible = true
                    val fieldObject = field.get(o)
                    return toDson(fieldObject)
                } catch (e: IllegalArgumentException) {
                    throw RuntimeException(e)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException(e)
                }
            }
        }
    }

    companion object {
        private val DSON = Dson()

        @JvmStatic
        val instance: Dson
            get() = DSON

        private fun longToByteArray(kvalue: Long): ByteArray {
            var value = kvalue
            val result = ByteArray(8)

            for (i in 7 downTo 0) {
                result[i] = (value and 0xffL).toByte()
                value = value shr 8
            }

            return result
        }
    }
}
