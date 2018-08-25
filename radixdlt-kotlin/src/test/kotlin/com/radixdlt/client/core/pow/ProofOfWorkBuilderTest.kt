package com.radixdlt.client.core.pow

import org.junit.Test

class ProofOfWorkBuilderTest {
    @Test
    @Throws(ProofOfWorkException::class)
    fun test() {
        val magic = 12345
        val seed = ByteArray(32)
        val pow = ProofOfWorkBuilder().build(magic, seed, 16)

        pow.validate()
    }
}
