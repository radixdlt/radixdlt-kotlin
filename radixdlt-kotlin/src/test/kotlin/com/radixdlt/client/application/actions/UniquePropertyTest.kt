package com.radixdlt.client.application.actions

import com.radixdlt.client.core.address.RadixAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock

class UniquePropertyTest {

    // TODO: Not necessary in Kotlin, remove when more mature
//    @Test
//    fun testNullUniqueProperty() {
//        val address = mock(RadixAddress::class.java)
//        assertThatThrownBy { UniqueProperty(null, address) }
//            .isInstanceOf(NullPointerException::class.java)
//        assertThatThrownBy { UniqueProperty(byteArrayOf(), null) }
//            .isInstanceOf(NullPointerException::class.java)
//    }

    @Test
    fun testConstruction() {
        val address = mock(RadixAddress::class.java)
        val uniqueProperty = UniqueProperty(byteArrayOf(), address)
        assertThat(uniqueProperty.address).isEqualTo(address)
    }
}
