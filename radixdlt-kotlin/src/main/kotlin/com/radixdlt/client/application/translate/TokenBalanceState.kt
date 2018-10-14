package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.RadixHash
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Consumable
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.util.mergeAfterFunction
import java.math.BigDecimal
import java.util.Collections
import java.util.HashMap

/**
 * All the token balances at an address at a given point in time.
 */
class TokenBalanceState {

    private val balance: Map<TokenRef, Balance>

    class Balance {
        private val balance: Long
        private val consumables: Map<RadixHash, Consumable>

        val amount: BigDecimal
            get() = TokenRef.subUnitsToDecimal(balance)

        internal constructor(balance: Long, consumables: Map<RadixHash, Consumable>) {
            this.balance = balance
            this.consumables = consumables
        }

        internal constructor(balance: Long, consumable: Consumable) {
            this.balance = balance
            this.consumables = Collections.singletonMap(RadixHash.of(consumable.getDson()), consumable)
        }

        fun unconsumedConsumables(): List<Consumable> {
            return consumables.entries.asSequence()
                .map { it.value }
                .filter { c -> c.getSpin() == Spin.UP }
                .toList()
        }

        companion object {

            fun empty(): Balance {
                return Balance(0L, emptyMap())
            }

            fun merge(balance: Balance, consumable: Consumable): Balance {
                val newMap = HashMap(balance.consumables)
                newMap[RadixHash.of(consumable.getDson())] = consumable
                val newBalance =
                    balance.balance + (if (consumable.getSpin() === Spin.UP) 1 else -1) * consumable.amount
                return Balance(newBalance, newMap)
            }
        }
    }

    constructor() {
        this.balance = emptyMap()
    }

    constructor(balance: Map<TokenRef, Balance>) {
        this.balance = balance
    }

    fun getBalance(): Map<TokenRef, Balance> {
        return Collections.unmodifiableMap(balance)
    }

    companion object {
        @JvmStatic
        fun merge(state: TokenBalanceState, consumable: Consumable): TokenBalanceState {
            val balance: HashMap<TokenRef, Balance> = HashMap(state.balance)
            balance.mergeAfterFunction(consumable.tokenRef, Balance(consumable.amount, consumable)) { bal1, _ ->
                    Balance.merge(bal1, consumable)
            }

            return TokenBalanceState(balance)
        }
    }
}
