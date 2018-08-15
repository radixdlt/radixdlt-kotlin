package com.radixdlt.client.wallet

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AbstractConsumable
import com.radixdlt.client.core.atoms.Consumable
import com.radixdlt.client.core.atoms.Particle
import com.radixdlt.client.core.atoms.TransactionAtom
import io.reactivex.ObservableEmitter
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap


class TransactionAtoms(private val address: RadixAddress, private val assetId: EUID) {

    private val unconsumedConsumables = ConcurrentHashMap<ByteBuffer, Consumable>()
    private val missingConsumable = ConcurrentHashMap<ByteBuffer, TransactionAtom>()

    inner class TransactionAtomsUpdate internal constructor(val newValidTransactions: io.reactivex.Observable<TransactionAtom>) {
        fun getUnconsumedConsumables(): io.reactivex.Maybe<Collection<Consumable>> {
            return newValidTransactions.lastElement().map {
                unconsumedConsumables.values
            }
        }
    }

    private fun addConsumables(transactionAtom: TransactionAtom, emitter: ObservableEmitter<TransactionAtom>) {
        transactionAtom.particles!!.asSequence()
                .filter { it.isAbstractConsumable }
                .map { it.asAbstractConsumable }
                .filter { particle -> particle.ownersPublicKeys.asSequence().all { address.ownsKey(it) } }
                .filter { particle -> particle.assetId == assetId }
                .forEach { particle ->
                    val dson = ByteBuffer.wrap(particle.dson)
                    if (particle.isConsumable) {

                        unconsumedConsumables.computeSynchronisedFunction(dson) { _, current ->
                            if (current == null) {
                                particle.asConsumable
                            } else {
                                throw IllegalStateException()
                            }
                        }

                        val reanalyzeAtom = missingConsumable.remove(dson)
                        if (reanalyzeAtom != null) {
                            checkConsumers(reanalyzeAtom, emitter)
                        }
                    } else {
                        unconsumedConsumables.remove(dson) ?: throw IllegalStateException()
                    }
                }
    }

    private fun checkConsumers(transactionAtom: TransactionAtom, emitter: ObservableEmitter<TransactionAtom>) {
        val missing: ByteBuffer? = transactionAtom.particles!!.asSequence()
                .filter(Particle::isAbstractConsumable)
                .map(Particle::asAbstractConsumable)
                .filter { particle -> particle.ownersPublicKeys.asSequence().all(address::ownsKey) }
                .filter { particle -> particle.assetId == assetId }
                .filter(AbstractConsumable::isConsumer)
                .map(AbstractConsumable::dson)
                .map { ByteBuffer.wrap(it) }
                .filter { dson -> !unconsumedConsumables.containsKey(dson) }
                .firstOrNull()

        if (missing != null) {
            LOGGER.info("Missing consumable for atom: $transactionAtom")

            missingConsumable.computeSynchronisedFunction(missing) { _: ByteBuffer, current: TransactionAtom? ->
                if (current == null) {
                    transactionAtom
                } else {
                    throw IllegalStateException()
                }
            }

        } else {
            emitter.onNext(transactionAtom)
            addConsumables(transactionAtom, emitter)
        }
    }

    fun accept(transactionAtom: TransactionAtom): TransactionAtomsUpdate {
        val observable = io.reactivex.Observable.create<TransactionAtom> { emitter ->
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

fun <K, V> ConcurrentHashMap<K, V>.computeSynchronisedFunction(key: K, remappingFunction: (t: K, u: V?) -> V): V? {
    return synchronized(this) {
        val valueSynchronized = remappingFunction(key, this[key])
        this[key] = valueSynchronized
        valueSynchronized
    }
}
