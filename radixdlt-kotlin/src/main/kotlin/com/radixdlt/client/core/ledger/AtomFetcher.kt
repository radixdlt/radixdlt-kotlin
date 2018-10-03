package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.AtomObservation
import com.radixdlt.client.core.network.IncreasingRetryTimer
import com.radixdlt.client.core.network.RadixJsonRpcClient
import io.reactivex.Observable
import io.reactivex.Single
import org.slf4j.LoggerFactory

/**
 * Module responsible for selecting a node and fetching atoms and retrying if necessary.
 */
class AtomFetcher(
    /**
     * Mechanism by which to get a valid client to connect to.
     */
    private val clientSelector: (Long) -> (Single<RadixJsonRpcClient>)
) {

    fun fetchAtoms(destination: EUID): Observable<AtomObservation> {
        return clientSelector(destination.shard)
            .flatMapObservable { client -> client.getAtoms(destination) }
            .doOnError { LOGGER.warn("Error on getAllAtoms: {}", destination) }
            .retryWhen(IncreasingRetryTimer())
            .doOnSubscribe { LOGGER.info("Atom Query Subscribe: destination({})", destination) }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AtomFetcher::class.java)
    }
}
