package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import io.reactivex.Observable

interface AtomStore {
    fun getAtoms(address: RadixAddress): Observable<Atom>
}
