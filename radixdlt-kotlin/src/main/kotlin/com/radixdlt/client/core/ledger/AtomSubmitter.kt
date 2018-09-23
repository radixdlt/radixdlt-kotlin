package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import io.reactivex.Observable

interface AtomSubmitter {
    fun submitAtom(atom: Atom): Observable<AtomSubmissionUpdate>
}
