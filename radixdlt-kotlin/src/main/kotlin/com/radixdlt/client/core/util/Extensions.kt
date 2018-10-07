package com.radixdlt.client.core.util

import com.radixdlt.client.core.crypto.ECKeyPair

fun MutableMap<Set<ECKeyPair>, Long>.mergeAfterSum(key: Set<ECKeyPair>, value: Long): Long {
    val newValue = if (this.containsKey(key)) this.getValue(key) + value else value
    this[key] = newValue
    return newValue
}

fun <K, V> MutableMap<K, V>.mergeAfterFunction(key: K, value: V, function: (t: V, u: V) -> V): V {
    val newValue: V = if (this.containsKey(key)) function(this.getValue(key), value) else value
    this[key] = newValue
    return newValue
}
