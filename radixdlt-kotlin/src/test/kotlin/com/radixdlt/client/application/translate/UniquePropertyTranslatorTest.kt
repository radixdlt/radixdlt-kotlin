package com.radixdlt.client.application.translate

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UniquePropertyTranslatorTest {
    @Test
    fun nullPropertyTest() {
        val translator = UniquePropertyTranslator()
        assertThat(translator.map(null)).isEmpty()
    }
}
