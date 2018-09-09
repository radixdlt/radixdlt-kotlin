package com.radixdlt.client.application.translate

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.crypto.Encryptor
import com.radixdlt.client.util.any
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

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
        val atomBuilder = mock(AtomBuilder::class.java)

        val testObserver = TestObserver.create<Any>()
        dataStoreTranslator.translate(dataStore, atomBuilder).subscribe(testObserver)
        testObserver.assertNoErrors()
        testObserver.assertComplete()
        verify(atomBuilder, times(1)).setEncryptorParticle(any())
    }
}
