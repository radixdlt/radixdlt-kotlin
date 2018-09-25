package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.Atom
import io.reactivex.Observable

interface AtomStore {
    fun getAtoms(destination: EUID?): Observable<Atom>
}
