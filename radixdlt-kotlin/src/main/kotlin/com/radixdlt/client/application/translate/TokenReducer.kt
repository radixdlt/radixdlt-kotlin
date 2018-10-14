package com.radixdlt.client.application.translate

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Minted
import com.radixdlt.client.core.atoms.particles.TokenParticle
import com.radixdlt.client.core.ledger.ParticleStore
import com.radixdlt.client.core.util.computeIfAbsentSynchronisedFunction
import com.radixdlt.client.core.util.mergeAfterSum
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TokenReducer(private val particleStore: ParticleStore) {
    private val cache = ConcurrentHashMap<RadixAddress, Observable<Map<TokenRef, TokenState>>>()

    fun getState(address: RadixAddress): Observable<Map<TokenRef, TokenState>> {
        return cache.computeIfAbsentSynchronisedFunction(
            address
        ) { addr ->
            val tokenParticles = particleStore.getParticles(address)
                .filter { p -> p is TokenParticle }
                .map { p -> p as TokenParticle }
                .scanWith({ HashMap<String, TokenParticle>() }) { map, p ->
                    val newMap = HashMap(map)
                    newMap[p.tokenRef.iso] = p
                    newMap
                }

            val mintedTokens = particleStore.getParticles(address)
                .filter { p -> p is Minted }
                .map { p -> p as Minted }
                .scanWith({ HashMap<String, Long>() }) { map, p ->
                    val newMap = HashMap(map)
                    newMap.mergeAfterSum(p.tokenRef.iso, p.amount)
                    return@scanWith newMap
                }

            Observables.combineLatest(tokenParticles, mintedTokens) { tokens, minted ->
                tokens.entries.asSequence()
                    .associateBy(
                        { e -> e.value.tokenRef },
                        { e ->
                            val p = e.value
                            val subUnits = minted[p.tokenRef.iso] ?: 0L
                            val totalSupply = TokenRef.subUnitsToDecimal(subUnits)
                            TokenState(p.name, p.iso, p.description, totalSupply)
                        }
                    )
            }.debounce(1000, TimeUnit.MILLISECONDS)
        }
    }
}
