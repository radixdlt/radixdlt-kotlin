@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.radixdlt.client.core.util

import java.lang.Byte as JByte
import java.lang.Long as JLong

// Avoiding using any Java 8 for maximum compatibility with Android below replaces Long.Bytes
const val LONG_BYTES = JLong.SIZE / JByte.SIZE
