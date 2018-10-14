package com.radixdlt.client.application.translate

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.AtomFeeConsumable
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.pow.ProofOfWork
import com.radixdlt.client.core.pow.ProofOfWorkBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt

class PowFeeMapperTest {
    @Test
    fun testNormalMap() {
        val hash = mock<RadixHash>()
        whenever(hash.toByteArray()).thenReturn(byteArrayOf())
        val builder = mock<ProofOfWorkBuilder>()
        val pow = mock<ProofOfWork>()
        whenever(builder.build(anyInt(), any(), anyInt())).thenReturn(pow)

        val hasher = mock<(List<Particle>) -> (RadixHash)>()
        whenever(hasher(any())).thenReturn(hash)
        val powFeeMapper = PowFeeMapper(hasher, builder)

        val universe = mock<RadixUniverse>()
        val powToken = mock<TokenRef>()
        whenever(universe.powToken).thenReturn(powToken)

        val particles = powFeeMapper.map(emptyList(), universe, mock())
        assertThat(particles)
            .hasOnlyOneElementSatisfying { p ->
                assertThat(p).isInstanceOf(AtomFeeConsumable::class.java)
                val a = p as AtomFeeConsumable
                assertThat(a.tokenRef).isEqualTo(powToken)
            }

        verify(builder, times(1)).build(anyInt(), any(), anyInt())
        verify(hasher, times(1))(any())
    }
}
