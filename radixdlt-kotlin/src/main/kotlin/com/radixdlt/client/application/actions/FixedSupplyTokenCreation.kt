package com.radixdlt.client.application.actions

import com.radixdlt.client.core.atoms.AccountReference

class FixedSupplyTokenCreation(
    val accountReference: AccountReference,
    val name: String,
    val iso: String,
    val description: String,
    val fixedSupply: Long
)
