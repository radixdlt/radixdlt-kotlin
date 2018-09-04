package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.AtomBuilder
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.Mockito.mock

class UniquePropertyTranslatorTest {
    @Test
    fun nullPropertyTest() {
        val translator = UniquePropertyTranslator()
        val atomBuilder = mock(AtomBuilder::class.java)
        val testObserver = TestObserver.create<Any>()
        translator.translate(null, atomBuilder).subscribe(testObserver)
        testObserver.assertComplete()
    }
}
