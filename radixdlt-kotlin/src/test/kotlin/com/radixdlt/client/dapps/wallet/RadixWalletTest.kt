package com.radixdlt.client.dapps.wallet

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.util.any
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class RadixWalletTest {

    // TODO: Remove when more mature
    // Null tests not needed in Kotlin since null has to be explicitly allowed
    // and hence passing null as an argument wont compile
//    @Test
//    fun nullTest() {
//        val api = mock(RadixApplicationAPI::class.java)
//        val radixWallet = RadixWallet(api)
//        assertThatThrownBy { radixWallet.getXRDBalance(null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.getXRDTransactions(null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.transferXRD(1, null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.transferXRD(1, null, "hi") }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.transferXRDWhenAvailable(1, null) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { radixWallet.transferXRDWhenAvailable(1, null, "hi") }
//            .isInstanceOf(NullPointerException::class.java)
//    }

    @Test
    fun transferWhenAvailableTest() {
        val api = mock<RadixApplicationAPI>(RadixApplicationAPI::class.java)
        val result = mock<RadixApplicationAPI.Result>(RadixApplicationAPI.Result::class.java)
        `when`<Observable<Amount>>(api.getMyBalance(any<Asset>())).thenReturn(
            Observable.just(
                Amount.subUnitsOf(
                    1000,
                    Asset.TEST
                )
            )
        )
        `when`<RadixApplicationAPI.Result>(api.sendTokens(any(), any(), any(), any())).thenReturn(result)
        val radixWallet = RadixWallet(api)
        val radixAddress = mock<RadixAddress>(RadixAddress::class.java)
        assertThat(radixWallet.transferXRDWhenAvailable(1000, radixAddress)).isNotNull
    }
}
