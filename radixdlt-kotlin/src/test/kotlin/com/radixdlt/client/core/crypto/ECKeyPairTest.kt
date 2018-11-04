package com.radixdlt.client.core.crypto

import org.assertj.core.api.Assertions.assertThatThrownBy

import org.junit.Test

class ECKeyPairTest {
    @Test
    fun decryptBadEncryptedDataWithGoodEncryptedPrivateKeyShouldThrowCryptoException() {
        val keyPair = ECKeyPairGenerator.newInstance().generateKeyPair()
        val privateKey = ECKeyPairGenerator.newInstance().generateKeyPair()

        val encryptedPrivateKey = privateKey.encryptPrivateKey(keyPair.getPublicKey())

        assertThatThrownBy { keyPair.decrypt(byteArrayOf(0), encryptedPrivateKey) }
            .isInstanceOf(CryptoException::class.java)
    }
}
