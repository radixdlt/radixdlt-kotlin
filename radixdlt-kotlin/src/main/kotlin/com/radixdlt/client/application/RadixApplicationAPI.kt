package com.radixdlt.client.application

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.objects.EncryptedData
import com.radixdlt.client.application.objects.UnencryptedData
import com.radixdlt.client.application.translate.DataStoreTranslator
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
import io.reactivex.Single

class RadixApplicationAPI private constructor(val identity: RadixIdentity,
                                              private val ledger: RadixLedger,
                                              private val atomBuilderSupplier: () -> AtomBuilder) {

    private val dataStoreTranslator: DataStoreTranslator = DataStoreTranslator.instance

    val address: RadixAddress
        get() = ledger.getAddressFromPublicKey(identity.getPublicKey())

    class Result internal constructor(private val updates: Observable<AtomSubmissionUpdate>) {
        private val completable: Completable

        init {

            this.completable = updates.filter { it.isComplete }
                    .firstOrError()
                    .flatMapCompletable { update ->
                        if (update.getState() === AtomSubmissionState.STORED) {
                            return@flatMapCompletable Completable.complete()
                        } else {
                            return@flatMapCompletable Completable.error(RuntimeException(update.message))
                        }
                    }
        }

        fun toObservable(): Observable<AtomSubmissionUpdate> {
            return updates
        }

        fun toCompletable(): Completable {
            return completable
        }
    }

    fun getDecryptableData(address: RadixAddress): Observable<UnencryptedData> {
        return ledger.getAllAtoms(address.getUID(), ApplicationPayloadAtom::class.java)
                .flatMapMaybe { atom -> UnencryptedData.fromAtom(atom, identity) }
    }

    fun storeData(encryptedData: EncryptedData, address: RadixAddress): Result {
        val dataStore = DataStore(encryptedData, address)

        val atomBuilder = atomBuilderSupplier()
        val updates = dataStoreTranslator.translate(dataStore, atomBuilder)
                .andThen(Single.fromCallable { atomBuilder.buildWithPOWFee(ledger.magic, address.publicKey) } )
                .flatMap(identity::sign)
                .flatMapObservable(ledger::submitAtom)

        return Result(updates)
    }

    fun storeData(encryptedData: EncryptedData, address0: RadixAddress, address1: RadixAddress): Result {
        val dataStore = DataStore(encryptedData, address0, address1)

        val atomBuilder = atomBuilderSupplier()
        val updates = dataStoreTranslator.translate(dataStore, atomBuilder)
                .andThen(Single.fromCallable { atomBuilder.buildWithPOWFee(ledger.magic, address0.publicKey) } )
                .flatMap(identity::sign)
                .flatMapObservable(ledger::submitAtom)

        return Result(updates)
    }

    companion object {

        fun create(identity: RadixIdentity): RadixApplicationAPI {
            return RadixApplicationAPI(identity, RadixUniverse.instance.ledger, ::AtomBuilder)
        }

        fun create(identity: RadixIdentity, ledger: RadixLedger, atomBuilderSupplier: () -> AtomBuilder): RadixApplicationAPI {
            return RadixApplicationAPI(identity, ledger, atomBuilderSupplier)
        }
    }
}
