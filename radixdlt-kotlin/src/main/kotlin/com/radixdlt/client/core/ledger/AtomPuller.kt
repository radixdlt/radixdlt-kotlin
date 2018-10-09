package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import io.reactivex.disposables.Disposable

interface AtomPuller {
    fun pull(address: RadixAddress): Disposable
}
