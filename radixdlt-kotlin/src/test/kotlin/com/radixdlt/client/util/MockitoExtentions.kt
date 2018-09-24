package com.radixdlt.client.util

import org.mockito.Mockito

fun <T> any(): T {
    Mockito.any<T>() as T
    @Suppress("UNCHECKED_CAST")
    return null as T
}

fun <T> any(type: Class<T>) = Mockito.any<T>(type) as T

fun <T> eq(value: T) = Mockito.eq<T>(value) as T
