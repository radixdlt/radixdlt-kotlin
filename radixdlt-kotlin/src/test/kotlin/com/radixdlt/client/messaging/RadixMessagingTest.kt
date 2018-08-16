package com.radixdlt.client.messaging

import com.google.gson.JsonObject
import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.UnencryptedData
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.crypto.ECSignature
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.util.any
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.*

class RadixMessagingTest {

    @Test
    fun testBadMessage() {
        val key = mock(ECPublicKey::class.java)
        val myIdentity = mock(RadixIdentity::class.java)
        `when`(myIdentity.getPublicKey()).thenReturn(key)
        val observer = TestObserver.create<RadixMessage>()

        val signatures = mock(Map::class.java)
        val signature = mock(ECSignature::class.java)
        `when`<ECSignature>(signatures.get(any<Any>()) as ECSignature?).thenReturn(signature)
        val metaData = HashMap<String, Any>()
        metaData["application"] = RadixMessaging.APPLICATION_ID
        metaData["signatures"] = signatures
        metaData["timestamp"] = 0L

        val undecryptableData = mock(UnencryptedData::class.java)
        `when`(undecryptableData.metaData).thenReturn(metaData)
        `when`(undecryptableData.data).thenReturn(byteArrayOf(0, 1, 2, 3))

        val message = JsonObject()
        message.addProperty("from", "JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ")
        message.addProperty("to", "JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ")
        message.addProperty("content", "hello")
        val decryptableData = mock(UnencryptedData::class.java)
        `when`(decryptableData.metaData).thenReturn(metaData)
        `when`(decryptableData.data).thenReturn(message.toString().toByteArray())

        val api = mock(RadixApplicationAPI::class.java)
        val address = mock(RadixAddress::class.java)
        `when`(api.address).thenReturn(address)
        `when`(api.identity).thenReturn(myIdentity)
        `when`(api.getDecryptableData(any())).thenReturn(Observable.just(undecryptableData, decryptableData))

        val messaging = RadixMessaging(api)
        messaging.allMessages
                .subscribe(observer)

        observer.assertValueCount(1)
        observer.assertNoErrors()
    }
}
