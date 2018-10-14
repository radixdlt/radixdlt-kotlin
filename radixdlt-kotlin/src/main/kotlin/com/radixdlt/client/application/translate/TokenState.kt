package com.radixdlt.client.application.translate

import java.math.BigDecimal

class TokenState(
    private val name: String,
    private val iso: String,
    private val description: String,
    private val totalSupply: BigDecimal
) {

    override fun toString(): String {
        return ("Token($iso) name($name) description($description) totalSupply($totalSupply) maxSupply($totalSupply)")
    }
}
