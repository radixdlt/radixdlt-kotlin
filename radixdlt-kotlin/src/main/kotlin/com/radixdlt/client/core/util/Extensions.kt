package com.radixdlt.client.core.util

import com.radixdlt.client.core.crypto.ECKeyPair
import java.util.concurrent.ConcurrentHashMap

fun MutableMap<Set<ECKeyPair>, Long>.mergeAfterSum(key: Set<ECKeyPair>, value: Long): Long {
    val newValue = if (this.containsKey(key)) this.getValue(key) + value else value
    this[key] = newValue
    return newValue
}

fun MutableMap<ECKeyPair, Long>.mergeAfterSum(key: ECKeyPair, value: Long): Long {
    val newValue = if (this.containsKey(key)) this.getValue(key) + value else value
    this[key] = newValue
    return newValue
}

fun MutableMap<String, Long>.mergeAfterSum(key: String, value: Long): Long {
    val newValue = if (this.containsKey(key)) this.getValue(key) + value else value
    this[key] = newValue
    return newValue
}

fun <K, V> MutableMap<K, V>.mergeAfterFunction(key: K, value: V, function: (t: V, u: V) -> V): V {
    val newValue: V = if (this.containsKey(key)) function(this.getValue(key), value) else value
    this[key] = newValue
    return newValue
}

fun <K, V> ConcurrentHashMap<K, V>.computeFunction(key: K, remappingFunction: (t: K, u: V?) -> V): V {
    val value = remappingFunction(key, this[key])
    this[key] = value
    return value
}

fun <K, V> ConcurrentHashMap<K, V>.computeSynchronisedFunction(key: K, remappingFunction: (t: K, u: V?) -> V) {
    return synchronized(this) {
        val valueSynchronized = remappingFunction(key, this[key])
        this[key] = valueSynchronized
    }
}

/**
 * Implementation that doesn't block when map already
 * contains the value
 */
fun <K, V> ConcurrentHashMap<K, V>.computeIfAbsentFunction(key: K, mappingFunction: (t: K) -> V): V {
    return this[key] ?: run {
        val value = mappingFunction(key)
        this[key] = value
        return@run value
    }
}

/**
 * Synchronised that doesn't block when map already
 * contains the value
 */
fun <K, V> ConcurrentHashMap<K, V>.computeIfAbsentSynchronisedFunction(key: K, mappingFunction: (t: K) -> V): V {
    return this[key] ?: synchronized(this) {
        var valueSynchronized = get(key)
        if (valueSynchronized == null) {
            valueSynchronized = mappingFunction(key)
            this[key] = valueSynchronized
        }
        return@synchronized valueSynchronized!!
    }
}
