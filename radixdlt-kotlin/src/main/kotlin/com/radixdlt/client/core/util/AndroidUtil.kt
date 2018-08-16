package com.radixdlt.client.core.util

object AndroidUtil {

    // Taken from BitcoinJ implementation
    // https://github.com/bitcoinj/bitcoinj/blob/3cb1f6c6c589f84fe6e1fb56bf26d94cccc85429/core/src/main/java/org/bitcoinj/core/Utils.java#L573
    private var isAndroid = -1

    @JvmStatic
    val isAndroidRuntime: Boolean
        get() {
            if (isAndroid == -1) {
                val runtime = System.getProperty("java.runtime.name")
                isAndroid = if (runtime != null && runtime == "Android Runtime") 1 else 0
            }
            return isAndroid == 1
        }
}
