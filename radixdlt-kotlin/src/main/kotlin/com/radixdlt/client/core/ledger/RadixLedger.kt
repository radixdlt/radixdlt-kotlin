package com.radixdlt.client.core.ledger

import com.radixdlt.client.application.translate.computeIfAbsentSynchronisedFunction
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomValidationException
import com.radixdlt.client.core.network.AtomQuery
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import com.radixdlt.client.core.network.IncreasingRetryTimer
import com.radixdlt.client.core.network.RadixJsonRpcClient
import com.radixdlt.client.core.network.RadixNetwork
import com.radixdlt.client.core.network.WebSocketClient
import com.radixdlt.client.core.serialization.RadixJson
import io.reactivex.Observable
import io.reactivex.Single
import org.slf4j.LoggerFactory
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A RadixLedger wraps a RadixNetwork and abstracts away network logic providing
 * the main interface to interacting with a Radix Ledger, specifically reading
 * and writing atoms onto the Ledger.
 */
class RadixLedger(
    /**
     * The Universe we need peers for
     * TODO: is this the right place to have this?
     */
    private val config: RadixUniverseConfig,
    val radixNetwork: RadixNetwork
) {

    private val debug = AtomicBoolean(false)

    /**
     * Implementation of a data store for all atoms in a shard
     */
    private val cache = ConcurrentHashMap<EUID, Observable<Atom>>()

    private val inMemoryAtomStore = InMemoryAtomStore()

    val magic: Int = config.getMagic()

    fun setDebug(debug: Boolean) {
        this.debug.set(debug)
    }

    /**
     * Returns a cold observable of the first peer found which supports
     * a set short shards which intersects with a given set of shards.
     *
     * @param shards set of shards to find an intersection with
     * @return a cold observable of the first matching Radix client
     */
    private fun getRadixClient(shards: Set<Long>): Single<RadixJsonRpcClient> {
        return this.radixNetwork.getRadixClients(shards)
            .flatMapMaybe { client ->
                client.status
                    .filter { status -> status != WebSocketClient.RadixClientStatus.FAILURE }
                    .map { _ -> client }
                    .firstOrError()
                    .toMaybe()
            }
            .flatMapMaybe { client ->
                client.getUniverse()
                    .doOnSuccess { cliUniverse ->
                        if (config != cliUniverse) {
                            LOGGER.warn(
                                "{} has universe: {} but looking for {}",
                                client, cliUniverse.getHash(), config.getHash()
                            )
                        }
                    }
                    .map { config == it }
                    .filter { b -> b }
                    .map { _ -> client }
            }
            .firstOrError()
    }

    /**
     * Returns a cold observable of the first peer found which supports
     * a set short shards which intersects with a given shard
     *
     * @param shard a shards to find an intersection with
     * @return a cold observable of the first matching Radix client
     */
    private fun getRadixClient(shard: Long): Single<RadixJsonRpcClient> {
        return getRadixClient(setOf(shard))
    }

    fun fetchAtoms(destination: EUID): Observable<Atom> {
        return cache.computeIfAbsentSynchronisedFunction(destination) { euid ->
            val atomQuery = AtomQuery(euid, Atom::class.java)
            return@computeIfAbsentSynchronisedFunction getRadixClient(euid.shard)
                .flatMapObservable { client -> client.getAtoms(atomQuery) }
                .doOnError {
                    LOGGER.warn("Error on getAllAtoms: {}", euid)
                }
                .retryWhen(IncreasingRetryTimer())
                .filter { atom ->
                    return@filter try {
                        RadixAtomValidator.getInstance().validate(atom)
                        true
                    } catch (e: AtomValidationException) {
                        // TODO: Stop stream and mark client as untrustable
                        LOGGER.error(e.toString())
                        false
                    }
                }
                .doOnSubscribe {
                    LOGGER.info("Atom Query Subscribe: destination({})", destination)
                }
                .doOnNext { atom -> inMemoryAtomStore.store(destination, atom) }
                .publish()
                .refCount()
        }
    }

    /**
     * Returns an unending stream of atoms which are stored at a particular destination.
     *
     * @param destination destination (which determines shard) to query atoms for
     * @return an Atom Observable
     */
    fun getAllAtoms(destination: EUID?): Observable<Atom> {
        Objects.requireNonNull(destination!!)

        val disposable = fetchAtoms(destination).subscribe()

        return inMemoryAtomStore.getAtoms(destination).doOnDispose(disposable::dispose)
    }

    /**
     * Immediately submits an atom into the ledger without waiting for subscription. The returned
     * observable is a full replay of the status of the atom, from submission to acceptance by
     * the network.
     *
     * @param atom atom to submit into the ledger
     * @return Observable emitting status updates to submission
     */
    fun submitAtom(atom: Atom): Observable<AtomSubmissionUpdate> {
        val status = getRadixClient(atom.requiredFirstShard)
            .doOnSuccess { client -> LOGGER.info("Found client to submit atom: {}", client.location) }
            .doOnError { throwable ->
                LOGGER.warn("Error on submitAtom {}", atom.hid)
                throwable.printStackTrace()
            }
            .flatMapObservable { client -> client.submitAtom(atom) }
            .doOnError(Throwable::printStackTrace)
            .retryWhen(IncreasingRetryTimer())

        if (debug.get()) {
            try {
                RadixAtomValidator.getInstance().validate(atom)
            } catch (e: AtomValidationException) {
                LOGGER.error(e.toString())
            }

            return status.doOnNext { atomSubmissionUpdate ->
                if (atomSubmissionUpdate.getState() == AtomSubmissionState.VALIDATION_ERROR) {
                    LOGGER.error("{}\n{}", atomSubmissionUpdate.message, RadixJson.gson.toJson(atom))
                }
            }
        }

        val replay = status.replay()
        replay.connect()

        return replay
    }

    /**
     * Attempt to cleanup resources
     */
    fun close() {
        // TODO: unsubscribe from all atom observables
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RadixLedger::class.java)
    }
}
