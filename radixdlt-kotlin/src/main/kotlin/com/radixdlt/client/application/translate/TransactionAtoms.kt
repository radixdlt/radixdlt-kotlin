package com.radixdlt.client.application.translate

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomFeeConsumable
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.Spin
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class TransactionAtoms(private val address: RadixAddress, private val assetId: EUID) {

    private val unconsumedConsumables = ConcurrentHashMap<ByteBuffer, Consumable>()
    private val missingConsumable = ConcurrentHashMap<ByteBuffer, Atom>()

    inner class TransactionAtomsUpdate internal constructor(val newValidTransactions: Observable<Atom>) {
        fun getUnconsumedConsumables(): io.reactivex.Maybe<Collection<Consumable>> {
            return newValidTransactions.lastElement().map {
                unconsumedConsumables.values
            }
        }
    }

    private fun addConsumables(atom: Atom, emitter: ObservableEmitter<Atom>) {
        atom.getConsumables(Spin.DOWN).asSequence()
            .filter { particle -> particle.getOwnersPublicKeys().asSequence().all(address::ownsKey) }
            .filter { particle -> particle.getTokenClass() == assetId }
            .forEach { down ->
                val dson = ByteBuffer.wrap(down.getDson())
                unconsumedConsumables.remove(dson) ?: throw IllegalStateException("Missing consumable for consumer.")
            }

        atom.getConsumables(Spin.UP).asSequence()
            .filter { up -> up.getOwnersPublicKeys().asSequence().all(address::ownsKey) }
            .filter { up -> up.getTokenClass() == assetId }
            .forEach { up ->
                val dson = ByteBuffer.wrap(up.getDson())
                unconsumedConsumables.computeSynchronisedFunction(dson) { _, current ->
                    if (current == null) {
                        up
                    } else {
                        throw IllegalStateException("Consumable already exists.")
                    }
                }
                val reanalyzeAtom = missingConsumable.remove(dson)
                if (reanalyzeAtom != null) {
                    checkConsumers(reanalyzeAtom, emitter)
                }
            }
    }

    private fun checkConsumers(transactionAtom: Atom, emitter: ObservableEmitter<Atom>) {
        val missing: ByteBuffer? = transactionAtom.getConsumables(Spin.DOWN).asSequence()
            .filter { down -> down.getOwnersPublicKeys().asSequence().all(address::ownsKey) }
            .filter { down -> down.getTokenClass() == assetId }
            .map(Consumable::getDson)
            .map { ByteBuffer.wrap(it) }
            .filter { dson -> !unconsumedConsumables.containsKey(dson) }
            .firstOrNull()

        if (missing != null) {
            LOGGER.info("Missing consumable for atom: $transactionAtom")

            missingConsumable.computeSynchronisedFunction(missing) { _: ByteBuffer, current: Atom? ->
                if (current == null) {
                    transactionAtom
                } else {
                    throw IllegalStateException()
                }
            }
        } else {
            if (transactionAtom.getConsumables(Spin.UP).asSequence().all { p -> p is AtomFeeConsumable }) {
                return
            }

            emitter.onNext(transactionAtom)
            addConsumables(transactionAtom, emitter)
        }
    }

    fun accept(transactionAtom: Atom): TransactionAtomsUpdate {
        val observable = Observable.create<Atom> { emitter ->
            synchronized(this@TransactionAtoms) {
                checkConsumers(transactionAtom, emitter)
            }
            emitter.onComplete()
        }.replay()

        observable.connect()

        return TransactionAtomsUpdate(observable)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionAtoms::class.java)
    }
}

fun <K, V> ConcurrentHashMap<K, V>.computeFunction(key: K, remappingFunction: (t: K, u: V?) -> V): V {
    val value = remappingFunction(key, this[key])
    this[key] = value
    return value
}

fun <K, V> ConcurrentHashMap<K, V>.computeSynchronisedFunction(key: K, remappingFunction: (t: K, u: V?) -> V) {
    return synchronized(this) {
        val valueSynchronized = remappingFunction(key, this[key])
        this[key] = valueSynchronized
    }
}
