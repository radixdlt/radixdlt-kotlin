package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.application.actions.StoreDataAction
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.crypto.Encryptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StoreDataActionTranslatorTest {

    @Test
    fun testEncryptorCreation() {
        val dataStoreTranslator = DataStoreTranslator.instance
        val storeDataAction = mock<StoreDataAction>()
        val data = mock<Data>()
        val encryptor = mock<Encryptor>()
        whenever(data.bytes).thenReturn(byteArrayOf())
        whenever(data.encryptor).thenReturn(encryptor)
        whenever(storeDataAction.data).thenReturn(data)
        assertThat(dataStoreTranslator.map(storeDataAction)).size().isEqualTo(2)
    }
}
