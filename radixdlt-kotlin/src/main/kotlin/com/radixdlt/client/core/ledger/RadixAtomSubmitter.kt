package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.core.network.IncreasingRetryTimer
import com.radixdlt.client.core.network.RadixJsonRpcClient
import io.reactivex.Observable
import io.reactivex.Single
import org.slf4j.LoggerFactory

/**
 * Module responsible for a node request and then submission of an atom and retry
 * mechanism if it fails.
 */
class RadixAtomSubmitter(private val clientSelector: (Set<Long>) -> (Single<RadixJsonRpcClient>)) : AtomSubmitter {

    /**
     * Immediately submits an atom into the ledger without waiting for subscription. The returned
     * observable is a full replay of the status of the atom, from submission to acceptance by
     * the network.
     *
     * @param atom atom to submit into the ledger
     * @return Observable emitting status updates to submission
     */
    override fun submitAtom(atom: Atom): Observable<AtomSubmissionUpdate> {
        val status = clientSelector(atom.requiredFirstShard)
            .doOnSuccess { client -> LOGGER.info("Found client to submit atom {}: {}", atom.hid, client.location) }
            .doOnError { throwable ->
                LOGGER.warn("Error on submitAtom {} {}", atom.hid, throwable.message)
            }
            .flatMapObservable { client -> client.submitAtom(atom) }
            .doOnError(Throwable::printStackTrace)
            .retryWhen(IncreasingRetryTimer())

        val replay = status.replay()
        replay.connect()

        return replay
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RadixAtomSubmitter::class.java)
    }
}
