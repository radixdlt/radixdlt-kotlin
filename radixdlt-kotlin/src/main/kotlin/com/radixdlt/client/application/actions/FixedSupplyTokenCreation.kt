package com.radixdlt.client.application.actions

import com.radixdlt.client.core.atoms.AccountReference
import java.util.Objects

class FixedSupplyTokenCreation(
    val accountReference: AccountReference,
    val name: String,
    val iso: String,
    val description: String,
    val fixedSupply: Long
) {
    init {
        Objects.requireNonNull(accountReference)
        Objects.requireNonNull(iso)
        if (fixedSupply <= 0) {
            throw IllegalArgumentException("Fixed supply must be greater than 0.")
        }
    }
}
