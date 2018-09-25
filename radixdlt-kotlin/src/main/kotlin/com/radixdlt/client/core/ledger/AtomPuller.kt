package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import io.reactivex.disposables.Disposable

interface AtomPuller {
    fun pull(euid: EUID): Disposable
}
