package com.radixdlt.client.dapps.wallet

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.util.any
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.math.BigDecimal

class RadixWalletTest {

    // TODO: Remove when more mature
    // Null tests not needed in Kotlin since null has to be explicitly allowed
    // and hence passing null as an argument wont compile
//    @Test
//    fun nullTest() {
//        val api = mock(RadixApplicationAPI::class.java)
//        val radixWallet = RadixWallet(api)
//        assertThatThrownBy { radixWallet.getBalance(null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.getTransactions(null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.send(1, null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.send(1, "hi", null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.sendWhenAvailable(1, null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.sendWhenAvailable(1, "hi", null) }
//            .isInstanceOf(NullPointerException::class.java)˚
//    }

    @Test
    fun transferWhenAvailableTest() {
        val api = mock(RadixApplicationAPI::class.java)
        val result = mock(RadixApplicationAPI.Result::class.java)
        `when`(result.toCompletable()).thenReturn(Completable.complete())
        `when`(api.getMyBalance(any())).thenReturn(Observable.just(Amount.of(BigDecimal("1.0"), Asset.TEST)))
        `when`(api.sendTokens(any(), any(), any(), any<ByteArray>())).thenReturn(result)
        val radixWallet = RadixWallet(api)
        val radixAddress = mock(RadixAddress::class.java)
        val testObserver = TestObserver.create<Any>()
        radixWallet.sendWhenAvailable(BigDecimal("1.0"), radixAddress).toCompletable().subscribe(testObserver)
        testObserver.assertComplete()
    }
}
