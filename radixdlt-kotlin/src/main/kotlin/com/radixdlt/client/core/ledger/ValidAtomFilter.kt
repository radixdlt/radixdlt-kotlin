package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.particles.Particle
import com.radixdlt.client.core.atoms.particles.Spin
import com.radixdlt.client.core.serialization.Dson
import com.radixdlt.client.core.util.computeSynchronisedFunction
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class ValidAtomFilter(private val address: RadixAddress, private val serializer: Dson) {
    private val upParticles = ConcurrentHashMap<ByteBuffer, Particle>()
    private val missingUpParticles = ConcurrentHashMap<ByteBuffer, Atom>()

    private fun addAtom(atom: Atom, emitter: ObservableEmitter<Atom>) {
        atom.particles(Spin.DOWN)
            .filter{ d -> d.getAddresses().asSequence().all(address::ownsKey)}
            .forEach { down ->
                val dson = ByteBuffer.wrap(serializer.toDson(down))
                upParticles.remove(dson) ?: throw IllegalStateException("Missing UP particle for $down")
            }

        atom.particles(Spin.UP)
            .filter{ up -> !up.getAddresses().isEmpty() && up.getAddresses().asSequence().all(address::ownsKey) }
            .forEach { up ->
                val dson = ByteBuffer.wrap(serializer.toDson(up))
                upParticles.computeSynchronisedFunction(dson) { _, current ->
                    if (current == null) {
                        return@computeSynchronisedFunction up
                    } else {
                        throw IllegalStateException("UP particle already exists: $up")
                    }
                }

                val reanalyzeAtom = missingUpParticles.remove(dson)
                if (reanalyzeAtom != null) {
                    checkDownParticles(reanalyzeAtom, emitter)
                }
            }
    }

    private fun checkDownParticles(atom: Atom, emitter: ObservableEmitter<Atom>) {
        val missingUp = atom.particles(Spin.DOWN)
            .filter { p -> p.getAddresses().asSequence().all(address::ownsKey) }
            .map(serializer::toDson)
            .map(ByteBuffer::wrap)
            .firstOrNull { dson -> !upParticles.containsKey(dson) }

        if (missingUp != null) {
            LOGGER.info("Missing up particle for atom: $atom")

            missingUpParticles.computeSynchronisedFunction(missingUp) { _, current ->
                if (current == null) {
                    return@computeSynchronisedFunction atom
                } else {
                    throw IllegalStateException()
                }
            }
        } else {
            emitter.onNext(atom)
            addAtom(atom, emitter)
        }
    }

    fun filter(atom: Atom): Observable<Atom> {
        val observable = Observable.create<Atom> { emitter ->
            synchronized(this@ValidAtomFilter) {
                checkDownParticles(atom, emitter)
            }
            emitter.onComplete()
        }.replay()

        observable.connect()

        return observable
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ValidAtomFilter::class.java)
    }
}
