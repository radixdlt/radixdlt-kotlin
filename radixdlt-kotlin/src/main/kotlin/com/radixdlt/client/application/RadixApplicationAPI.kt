package com.radixdlt.client.application

import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.ledger.RadixLedger
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import io.reactivex.Completable
import io.reactivex.Observable

class RadixApplicationAPI private constructor(val identity: RadixIdentity, private val ledger: RadixLedger) {

    val address: RadixAddress
        get() = ledger.getAddressFromPublicKey(identity.getPublicKey())

    class Result internal constructor(private val updates: Observable<AtomSubmissionUpdate>) {
        private val completable: Completable

        init {

            this.completable = updates.filter { it.isComplete }
                    .firstOrError()
                    .flatMapCompletable { update ->
                        if (update.state === AtomSubmissionState.STORED) {
                            return@flatMapCompletable Completable.complete()
                        } else {
                            return@flatMapCompletable Completable.error(RuntimeException(update.message))
                        }
                    }
        }

        fun toCompletable(): Completable {
            return completable
        }
    }

    fun getEncryptedData(address: RadixAddress): Observable<EncryptedData> {
        return ledger.getAllAtoms(address.getUID(), ApplicationPayloadAtom::class.java)
                .filter { atom -> atom.encryptor?.protectors != null }
                .map { EncryptedData.fromAtom(it) }
    }

    fun getDecryptableData(address: RadixAddress): Observable<UnencryptedData> {
        return ledger.getAllAtoms(address.getUID(), ApplicationPayloadAtom::class.java)
                .flatMapMaybe { atom -> UnencryptedData.fromAtom(atom, identity) }
    }

    fun storeData(encryptedData: EncryptedData, address: RadixAddress): Result {
        val storeDataAction = StoreDataAction(encryptedData, address)

        val atomBuilder = AtomBuilder()
        storeDataAction.addToAtomBuilder(atomBuilder)
        val unsignedAtom = atomBuilder.buildWithPOWFee(ledger.magic, address.publicKey)

        return Result(identity.sign(unsignedAtom).flatMapObservable(ledger::submitAtom))
    }

    fun storeData(encryptedData: EncryptedData, address0: RadixAddress, address1: RadixAddress): Result {
        val storeDataAction = StoreDataAction(encryptedData, address0, address1)

        val atomBuilder = AtomBuilder()
        storeDataAction.addToAtomBuilder(atomBuilder)
        val unsignedAtom = atomBuilder.buildWithPOWFee(ledger.magic, address0.publicKey)

        return Result(identity.sign(unsignedAtom).flatMapObservable(ledger::submitAtom))
    }

    companion object {
        @JvmStatic
        fun create(identity: RadixIdentity): RadixApplicationAPI {
            return RadixApplicationAPI(identity, RadixUniverse.instance.ledger)
        }

        @JvmStatic
        fun create(identity: RadixIdentity, ledger: RadixLedger): RadixApplicationAPI {
            return RadixApplicationAPI(identity, ledger)
        }
    }
}
