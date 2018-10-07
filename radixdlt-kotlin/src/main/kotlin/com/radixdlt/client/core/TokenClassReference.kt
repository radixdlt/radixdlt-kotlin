package com.radixdlt.client.core

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.address.EUID

class TokenClassReference(
    @field:SerializedName("token_id")
    val token: EUID,
    private val revision: EUID
)
