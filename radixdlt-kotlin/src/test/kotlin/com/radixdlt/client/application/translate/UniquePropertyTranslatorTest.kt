package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.mock
import com.radixdlt.client.core.atoms.AtomBuilder
import org.junit.Test

class UniquePropertyTranslatorTest {
    @Test
    fun nullPropertyTest() {
        val translator = UniquePropertyTranslator()
        val atomBuilder = mock<AtomBuilder>()
        translator.translate(null, atomBuilder)
    }
}
