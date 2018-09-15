package com.radixdlt.client.core.atoms

import com.google.gson.annotations.SerializedName
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.crypto.ECKeyPair

class AssetAtom(
    private val owners: Set<ECKeyPair>,
    private val id: EUID,
    private val type: String,
    @field:SerializedName("sub_units")
    private val subUnits: Long,
    private val maximumUnits: Long,
    private val settings: Long,
    private val iso: String,
    private val label: String,
    private val description: String,
    private val classification: String,
    private val icon: ByteArray
) : Atom()
