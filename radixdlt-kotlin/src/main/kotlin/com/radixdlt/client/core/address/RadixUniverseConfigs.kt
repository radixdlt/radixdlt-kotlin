package com.radixdlt.client.core.address

import java.io.InputStream


object RadixUniverseConfigs {

    @JvmStatic
    val betanet: RadixUniverseConfig
        get() = RadixUniverseConfig.fromInputStream(getConfigFileStream("betanet.json"))

    @JvmStatic
    val winterfell: RadixUniverseConfig
        get() = RadixUniverseConfig.fromInputStream(getConfigFileStream("testuniverse.json"))

    @JvmStatic
    val sunstone: RadixUniverseConfig
        get() = RadixUniverseConfig.fromInputStream(getConfigFileStream("testuniverse.json"))

    @JvmStatic
    val highgarden: RadixUniverseConfig
        get() = RadixUniverseConfig.fromInputStream(getConfigFileStream("highgarden.json"))

    @JvmStatic
    val alphanet: RadixUniverseConfig
        get() = RadixUniverseConfig.fromInputStream(getConfigFileStream("alphanet.json"))

    private fun getConfigFileStream(name: String): InputStream {
        val source = "/universe/$name"
        return RadixUniverseConfig::class.java.getResourceAsStream(source)
    }
}
