package com.radixdlt.client.application.translate

import java.math.BigDecimal

/**
 * The state and data of a token at a given moment in time
 */
class TokenState(
    val name: String?,
    val iso: String?,
    val description: String?,
    val totalSupply: BigDecimal
) {

    val maxSupply: BigDecimal
        get() = totalSupply

    override fun toString(): String {
        return ("Token($iso) name($name) description($description) totalSupply($totalSupply) maxSupply($totalSupply)")
    }
}
