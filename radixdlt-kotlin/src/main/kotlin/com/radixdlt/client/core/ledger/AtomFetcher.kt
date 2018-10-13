package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomValidationException
import com.radixdlt.client.core.network.AtomQuery
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

    fun fetchAtoms(address: RadixAddress): Observable<Atom> {
        val atomQuery = AtomQuery(address.getUID())
        return Observable.fromCallable { clientSelector(address.getUID().shard) }
        .flatMapSingle { c -> c }
        .flatMap { client -> client.getAtoms(atomQuery) }
        .doOnError { LOGGER.warn("Error on getAllAtoms: {}", address) }
        .retryWhen(IncreasingRetryTimer())
        .filter { atom ->
            LOGGER.info("Received atom ${atom.hid}")
            return@filter try {
                RadixAtomValidator.getInstance().validate(atom)
                true
            } catch (e: AtomValidationException) {
                // TODO: Stop stream and mark client as untrustable
                LOGGER.error(e.toString())
                false
            }
        }
        .doOnSubscribe { LOGGER.info("Atom Query Subscribe: address({})", address) }
    }
    companion object {
        private val LOGGER = LoggerFactory.getLogger(AtomFetcher::class.java)
    }
}
