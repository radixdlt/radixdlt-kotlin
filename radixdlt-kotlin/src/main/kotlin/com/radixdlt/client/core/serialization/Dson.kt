package com.radixdlt.client.core.serialization

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.util.Base64Encoded
import okio.ByteString
import org.bouncycastle.util.encoders.Base64
import org.bouncycastle.util.encoders.Hex
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.HashMap

class Dson private constructor() {

    private val versionField = object : DsonField {
        override val name: String
            get() = "version"

        override val bytes: ByteArray
            get() = toDson(100L)
    }

    private enum class Primitive(val value: Int) {
        NUMBER(2),
        STRING(3),
        BYTES(4),
        OBJECT(5),
        ARRAY(6),
        EUID(7),
        HASH(8)
    }

    private fun parse(byteBuffer: ByteBuffer): JsonElement {
        val type = byteBuffer.get().toInt()
        var length = byteBuffer.int
        val result: JsonElement
        if (type == Primitive.NUMBER.value) {
            result = JsonPrimitive(byteBuffer.long)
        } else if (type == Primitive.STRING.value) {
            val buffer = ByteArray(length)
            byteBuffer.get(buffer)
            result = JsonPrimitive(String(buffer))
        } else if (type == Primitive.BYTES.value) {
            val buffer = ByteArray(length)
            byteBuffer.get(buffer)
            val jsonObject = JsonObject()
            jsonObject.addProperty("serializer", "BASE64")
            jsonObject.addProperty("value", Base64.toBase64String(buffer))
            result = jsonObject
        } else if (type == Primitive.OBJECT.value) {
            val jsonObject = JsonObject()

            while (length > 0) {
                val fieldNameLength = byteBuffer.get().toInt()
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
            val jsonObject = JsonObject()
            jsonObject.addProperty("serializer", "EUID")
            val buffer = ByteArray(length)
            byteBuffer.get(buffer)
            jsonObject.addProperty("value", Hex.toHexString(buffer))
            result = jsonObject
        } else if (type == Primitive.HASH.value) {
            val jsonObject = JsonObject()
            jsonObject.addProperty("serializer", "HASH")
            val buffer = ByteArray(length)
            byteBuffer.get(buffer)
            jsonObject.addProperty("value", ByteString.of(*buffer).hex())
            result = jsonObject
        } else {
            throw RuntimeException("Unknown type: $type")
        }

        return result
    }

    fun parse(buffer: ByteArray): JsonElement {
        return parse(ByteBuffer.wrap(buffer))
    }

    interface DsonField {
        val name: String
        val bytes: ByteArray
    }

    fun toDson(o: Any?): ByteArray {
        val raw: ByteArray
        val type: Byte

        if (o == null) {
            throw IllegalArgumentException("Null sent")
        } else if (o is Collection<*>) {
            val outputStream = ByteArrayOutputStream()
            for (arrayObject in o) {
                try {
                    val arrayObjRaw = toDson(arrayObject)
                    outputStream.write(arrayObjRaw)
                } catch (e: IOException) {
                    throw RuntimeException()
                }
            }
            raw = outputStream.toByteArray()
            type = 6
        } else if (o is Long) {
            raw = longToByteArray((o as Long?)!!)
            type = 2
        } else if (o is EUID) {
            raw = o.toByteArray()
            type = 7
        } else if (o is Base64Encoded) {
            raw = o.toByteArray()
            type = 4
        } else if (o is String) {
            raw = o.toByteArray()
            type = 3
        } else if (o is ByteArray) {
            raw = o
            type = 4
        } else if (o is Map<*, *>) {
            val map = o as Map<*, *>?

            if (HashMap::class.java == o.javaClass) {
                throw IllegalStateException("Cannot DSON serialize HashMap. Must be a predictably ordered map.")
            }

            val fieldStream: Sequence<DsonField> = map!!.keys.asSequence().map { key ->
                object : DsonField {
                    override val name: String
                        get() = key.toString()

                    override val bytes: ByteArray
                        get() = toDson(map[key])
                }
            }

            val rawList: Sequence<DsonField> = fieldStream.sortedBy { it.name }
            raw = toByteArray(rawList)
            type = 5
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
                .map {
                    object : DsonField {
                        override val name: String
                            get() {
                                val serializedName = it.getAnnotation(SerializedName::class.java)
                                return serializedName?.value ?: it.name
                            }
                        override val bytes: ByteArray
                            get() {
                                try {
                                    it.isAccessible = true
                                    val fieldObject = it.get(o)
                                    return toDson(fieldObject)
                                } catch (e: IllegalAccessException) {
                                    throw RuntimeException()
                                }
                            }
                    }
                }

            val rawList = fieldStream.asSequence().plus(versionField).sortedBy { it.name }
            raw = toByteArray(rawList)
            type = 5
        }

        val byteBuffer = ByteBuffer.allocate(5 + raw.size)
        byteBuffer.put(type)
        byteBuffer.putInt(raw.size)
        byteBuffer.put(raw)

        return byteBuffer.array()
    }

    fun toByteArray(rawList: Sequence<DsonField>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        rawList.forEach { dsonField ->
            try {
                val nameBytes = dsonField.name.toByteArray(StandardCharsets.UTF_8)
                outputStream.write(nameBytes.size)
                outputStream.write(nameBytes)
                outputStream.write(dsonField.bytes)
            } catch (e: IOException) {
                throw RuntimeException()
            }
        }
        return outputStream.toByteArray()
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
