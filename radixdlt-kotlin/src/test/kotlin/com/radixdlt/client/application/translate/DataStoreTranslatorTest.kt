package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.crypto.Encryptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class DataStoreTranslatorTest {

    @Test
    fun testEncryptorCreation() {
        val dataStoreTranslator = DataStoreTranslator.instance
        val dataStore = mock(DataStore::class.java)
        val data = mock(Data::class.java)
        val encryptor = mock(Encryptor::class.java)
        `when`<ByteArray>(data.bytes).thenReturn(byteArrayOf())
        `when`<Encryptor>(data.encryptor).thenReturn(encryptor)
        `when`(dataStore.data).thenReturn(data)
        assertThat(dataStoreTranslator.map(dataStore)).size().isEqualTo(2)
    }
}
