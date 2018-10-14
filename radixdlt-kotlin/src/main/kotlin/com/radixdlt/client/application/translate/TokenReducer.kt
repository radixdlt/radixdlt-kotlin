package com.radixdlt.client.application.translate

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Minted
import com.radixdlt.client.core.atoms.particles.TokenParticle
import com.radixdlt.client.core.ledger.ParticleStore
import com.radixdlt.client.core.util.computeIfAbsentSynchronisedFunction
import com.radixdlt.client.core.util.mergeAfterFunction
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TokenReducer(private val particleStore: ParticleStore) {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Map<TokenRef, TokenState>>>()

    fun getState(address: RadixAddress): Observable<Map<TokenRef, TokenState>> {
        return cache.computeIfAbsentSynchronisedFunction(address) { _ ->
            particleStore.getParticles(address)
                .filter { p -> p is TokenParticle || p is Minted }
                .scanWith( { HashMap<TokenRef, TokenState>() } ) { map, p ->
                    val newMap: HashMap<TokenRef, TokenState> = HashMap(map)
                    if (p is TokenParticle) {
                        val tokenState = TokenState(p.name, p.iso, p.description, BigDecimal.ZERO)

                        newMap.mergeAfterFunction(p.tokenRef, tokenState) { a, b ->
                            TokenState(b.name, b.iso, b.description, a.totalSupply)
                        }

                    } else if (p is Minted) {
                        val tokenState = TokenState(null, p.tokenRef.iso, null, BigDecimal.ZERO)

                        newMap.mergeAfterFunction(p.tokenRef, tokenState) { a, b ->
                            TokenState(b.name, b.iso, b.description, a.totalSupply.add(b.totalSupply))
                        }
                    }

                    return@scanWith newMap

                }
                .map { m -> m as Map<TokenRef, TokenState> }
                .debounce(1000, TimeUnit.MILLISECONDS)
        }
    }
}
