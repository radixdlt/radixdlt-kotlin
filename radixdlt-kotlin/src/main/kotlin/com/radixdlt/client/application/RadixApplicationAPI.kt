package com.radixdlt.client.application

import com.radixdlt.client.application.actions.DataStore
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.actions.UniqueProperty
import com.radixdlt.client.application.identity.RadixIdentity
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.application.objects.UnencryptedData
import com.radixdlt.client.application.translate.ApplicationStore
import com.radixdlt.client.application.translate.DataStoreTranslator
import com.radixdlt.client.application.translate.TokenBalanceReducer
import com.radixdlt.client.application.translate.TokenBalanceState
import com.radixdlt.client.application.translate.TokenReducer
import com.radixdlt.client.application.translate.TokenState
import com.radixdlt.client.application.translate.TokenTransferTranslator
import com.radixdlt.client.application.translate.UniquePropertyTranslator
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.AccountReference
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Minted
import com.radixdlt.client.core.atoms.particles.TokenParticle
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import com.radixdlt.client.core.network.AtomSubmissionUpdate.AtomSubmissionState
import com.radixdlt.client.core.serialization.RadixJson
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.annotations.Nullable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.observables.ConnectableObservable
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.Objects

/**
 * The Radix Dapp API, a high level api which dapps can utilize. The class hides
 * the complexity of Atoms and cryptography and exposes a simple high level interface.
 */
class RadixApplicationAPI private constructor(
    val myIdentity: RadixIdentity,
    private val universe: RadixUniverse,
    // TODO: Translators from application to particles
    private val dataStoreTranslator: DataStoreTranslator,
    // TODO: Translator from particles to atom
    private val atomBuilderSupplier: () -> AtomBuilder,
    private val ledger: RadixUniverse.Ledger
) {

    private val tokenTransferTranslator = TokenTransferTranslator(universe)
    private val uniquePropertyTranslator = UniquePropertyTranslator()

    private val tokenStore = ApplicationStore(ledger.getParticleStore(), TokenReducer())
    private val tokenBalanceStore = ApplicationStore(ledger.getParticleStore(), TokenBalanceReducer())

    val myAddress: RadixAddress
        get() = universe.getAddressFrom(myIdentity.getPublicKey())

    val myPublicKey: ECPublicKey
        get() = myIdentity.getPublicKey()

    class Result internal constructor(private val updates: Observable<AtomSubmissionUpdate>) {
        private val completable: Completable

        init {
            this.completable = updates.filter { it.isComplete }
                .firstOrError()
                .flatMapCompletable { update ->
                    if (update.getState() == AtomSubmissionState.STORED) {
                        return@flatMapCompletable Completable.complete()
                    } else {
                        return@flatMapCompletable Completable.error(RuntimeException(update.data.toString()))
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

    /**
     * Idempotent method which prefetches atoms in user's account
     * TODO: what to do when no puller available
     *
     * @return Disposable to dispose to stop pulling
     */
    fun pull(): Disposable {
        return pull(myAddress)
    }

    /**
     * Idempotent method which prefetches atoms in an address
     * TODO: what to do when no puller available
     *
     * @param address the address to pull atoms from
     * @return Disposable to dispose to stop pulling
     */
    fun pull(address: RadixAddress): Disposable {
        Objects.requireNonNull(address)

        return if (ledger.getAtomPuller() != null) {
            ledger.getAtomPuller()!!.pull(address)
        } else {
            Disposables.disposed()
        }
    }

    fun getNativeToken(): TokenRef {
        return universe.nativeToken
    }

    fun getNativeTokenState(): Observable<TokenState> {
        return getToken(getNativeToken())
    }

    fun getTokens(address: RadixAddress): Observable<Map<TokenRef, TokenState>> {
        pull(address)

        return tokenStore.getState(address)
    }

    fun getMyTokens(): Observable<Map<TokenRef, TokenState>> {
        return getTokens(myAddress)
    }

    fun getToken(ref: TokenRef): Observable<TokenState> {
        pull(universe.getAddressFrom(ref.address.getKey()))

        return tokenStore.getState(universe.getAddressFrom(ref.address.getKey()))
            .flatMapMaybe { m ->
                m[ref]?.let { Maybe.just(it) } ?: Maybe.empty()
            }
    }

    fun getData(address: RadixAddress): Observable<Data> {
        Objects.requireNonNull(address)

        pull(address)

        return ledger.getAtomStore().getAtoms(address)
            .map(dataStoreTranslator::fromAtom)
            .flatMapMaybe { data -> if (data is Data) Maybe.just(data) else Maybe.empty() }
    }

    fun getReadableData(address: RadixAddress): Observable<UnencryptedData> {
        return getData(address)
            .flatMapMaybe { data -> myIdentity.decrypt(data).toMaybe().onErrorComplete() }
    }

    fun storeData(data: Data): Result {
        return this.storeData(data, myAddress)
    }

    fun storeData(data: Data, address: RadixAddress): Result {
        val dataStore = DataStore(data, address)

        val atomBuilder = atomBuilderSupplier()
        val updates: ConnectableObservable<AtomSubmissionUpdate> = dataStoreTranslator.translate(dataStore, atomBuilder)
            .andThen(Single.fromCallable {
                atomBuilder.buildWithPOWFee(universe.magic, address.publicKey, universe.powToken)
            })
            .flatMap(myIdentity::sign)
            .flatMapObservable(ledger.getAtomSubmitter()::submitAtom)
            .replay()

        updates.connect()

        return Result(updates)
    }

    fun storeData(data: Data, address0: RadixAddress, address1: RadixAddress): Result {
        val dataStore = DataStore(data, address0, address1)

        val atomBuilder = atomBuilderSupplier()
        val updates: ConnectableObservable<AtomSubmissionUpdate> = dataStoreTranslator.translate(dataStore, atomBuilder)
            .andThen(Single.fromCallable {
                atomBuilder.buildWithPOWFee(universe.magic, address0.publicKey, universe.powToken)
            })
            .flatMap(myIdentity::sign)
            .flatMapObservable(ledger.getAtomSubmitter()::submitAtom)
            .replay()

        updates.connect()

        return Result(updates)
    }

    fun getMyTokenTransfers(): Observable<TokenTransfer> {
        return getTokenTransfers(myAddress)
    }

    fun getTokenTransfers(address: RadixAddress): Observable<TokenTransfer> {
        Objects.requireNonNull(address)

        pull(address)

        return ledger.getAtomStore().getAtoms(address)
            .flatMapIterable(tokenTransferTranslator::fromAtom)
    }

    fun getBalance(address: RadixAddress): Observable<Map<TokenRef, BigDecimal>> {
        Objects.requireNonNull(address)

        pull(address)

        return tokenBalanceStore.getState(address)
            .map(TokenBalanceState::getBalance)
            .map { map ->
                map.entries.asSequence().associateBy(Map.Entry<TokenRef, TokenBalanceState.Balance>::key) { e ->
                    e.value.amount
                }
            }
    }

    fun getMyBalance(tokenReference: TokenRef): Observable<BigDecimal> {
        return getBalance(myAddress, tokenReference)
    }

    fun getBalance(address: RadixAddress, token: TokenRef): Observable<BigDecimal> {
        Objects.requireNonNull(token)
        return getBalance(address)
            .map { balances ->
                balances[token] ?: BigDecimal.ZERO
            }
    }

    // TODO: refactor to access a TokenTranslator
    fun createFixedSupplyToken(name: String, iso: String, description: String, fixedSupply: Long): Result {
        val account = AccountReference(myPublicKey)
        val token = TokenParticle(
            account,
            name,
            iso,
            description,
            TokenParticle.MintPermissions.SAME_ATOM_ONLY,
            null
        )
        val minted = Minted(
            fixedSupply * TokenRef.SUB_UNITS,
            account,
            System.currentTimeMillis(),
            token.tokenRef,
            System.currentTimeMillis() / 60000L + 60000
        )

        val unsignedAtom = atomBuilderSupplier()
            .addParticle(token)
            .addParticle(minted)
            .buildWithPOWFee(universe.magic, myPublicKey, universe.powToken)

        val updates = myIdentity.sign(unsignedAtom)
            .flatMapObservable {
                ledger.getAtomSubmitter().submitAtom(it)
            }
            .replay()

        updates.connect()

        return Result(updates)
    }

    /**
     * Sends an amount of a token to an address
     *
     * @param to the address to send tokens to
     * @param amount the amount and token type
     * @return result of the transaction
     */
    fun sendTokens(to: RadixAddress, amount: BigDecimal, token: TokenRef): Result {
        return transferTokens(myAddress, to, amount, token)
    }

    /**
     * Sends an amount of a token with a message attachment to an address
     *
     * @param to the address to send tokens to
     * @param amount the amount and token type
     * @param message message to be encrypted and attached to transfer
     * @return result of the transaction
     */
    fun sendTokensWithMessage(to: RadixAddress, amount: BigDecimal, token: TokenRef, message: String?): Result {
        return sendTokensWithMessage(to, amount, token, message, null)
    }

    /**
     * Sends an amount of a token with a message attachment to an address
     *
     * @param to the address to send tokens to
     * @param amount the amount and token type
     * @param message message to be encrypted and attached to transfer
     * @return result of the transaction
     */
    fun sendTokensWithMessage(
        to: RadixAddress,
        amount: BigDecimal,
        token: TokenRef,
        message: String?,
        unique: ByteArray?
    ): Result {
        val attachment: Data?
        if (message != null) {
            attachment = Data.DataBuilder()
                .addReader(to.publicKey)
                .addReader(myPublicKey)
                .bytes(message.toByteArray()).build()
        } else {
            attachment = null
        }

        return transferTokens(myAddress, to, amount, token, attachment, unique)
    }

    /**
     * Sends an amount of a token with a data attachment to an address
     *
     * @param to the address to send tokens to
     * @param amount the amount and token type
     * @param attachment the data attached to the transaction
     * @return result of the transaction
     */
    fun sendTokens(
        to: RadixAddress,
        amount: BigDecimal,
        token: TokenRef,
        attachment: Data?
    ): Result {
        return transferTokens(myAddress, to, amount, token, attachment)
    }

    /**
     * Sends an amount of a token with a data attachment to an address with a unique property
     * meaning that no other transaction can be executed with the same unique bytes
     *
     * @param to the address to send tokens to
     * @param amount the amount and token type
     * @param attachment the data attached to the transaction
     * @param unique the bytes representing the unique id of this transaction
     * @return result of the transaction
     */
    fun sendTokens(
        to: RadixAddress,
        amount: BigDecimal,
        token: TokenRef,
        attachment: Data?,
        unique: ByteArray?
    ): Result {
        return transferTokens(myAddress, to, amount, token, attachment, unique)
    }

    fun transferTokens(from: RadixAddress, to: RadixAddress, amount: BigDecimal, token: TokenRef): Result {
        return transferTokens(from, to, amount, token, null)
    }

    fun transferTokens(
        from: RadixAddress,
        to: RadixAddress,
        amount: BigDecimal,
        token: TokenRef,
        attachment: Data?
    ): Result {
        return transferTokens(from, to, amount, token, attachment, null)
    }

    fun transferTokens(
        from: RadixAddress,
        to: RadixAddress,
        amount: BigDecimal,
        token: TokenRef,
        attachment: Data?,
        unique: ByteArray? // TODO: make unique immutable
    ): Result {
        Objects.requireNonNull(from)
        Objects.requireNonNull(to)
        Objects.requireNonNull(amount)
        Objects.requireNonNull(token)

        val tokenTransfer = TokenTransfer.create(from, to, amount, token, attachment)
        val uniqueProperty: UniqueProperty?
        if (unique != null) {
            // Unique Property must be the from address so that all validation occurs in a single shard.
            // Once multi-shard validation is implemented this constraint can be removed.
            uniqueProperty = UniqueProperty(unique, from)
        } else {
            uniqueProperty = null
        }

        return executeTransaction(tokenTransfer, uniqueProperty)
    }

    // TODO: make this more generic
    private fun executeTransaction(tokenTransfer: TokenTransfer, @Nullable uniqueProperty: UniqueProperty?): Result {
        Objects.requireNonNull(tokenTransfer)

        pull(tokenTransfer.from!!)

        val atomBuilder = atomBuilderSupplier()

        uniquePropertyTranslator.translate(uniqueProperty, atomBuilder)
        val unsignedAtom = tokenBalanceStore.getState(tokenTransfer.from)
            .firstOrError()
            .map { curState -> tokenTransferTranslator.translate(curState, tokenTransfer, atomBuilder) }
            .map { builder ->
                builder.buildWithPOWFee(
                    universe.magic,
                    tokenTransfer.from.publicKey,
                    universe.powToken
                )
            }

        val updates = unsignedAtom
            .flatMap(myIdentity::sign)
            .flatMapObservable(ledger.getAtomSubmitter()::submitAtom)
            .doOnNext {update ->
                //TODO: retry on collision
                if (update.getState() == AtomSubmissionState.COLLISION) {
                    val data = update.data!!.asJsonObject
                    val jsonPointer = data.getAsJsonPrimitive("pointerToConflict").asString
                    LOGGER.info("ParticleConflict: pointer({}) cause({}) atom({})",
                        jsonPointer,
                        data.getAsJsonPrimitive("cause").asString,
                        RadixJson.gson.toJson(update.atom)
                    )
                }
            }
            .replay()

        updates.connect()

        return Result(updates)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(RadixApplicationAPI::class.java)

        @JvmStatic
        fun create(identity: RadixIdentity): RadixApplicationAPI {
            Objects.requireNonNull(identity)
            return create(
                identity,
                RadixUniverse.getInstance(),
                DataStoreTranslator.instance,
                ::AtomBuilder
            )
        }

        @JvmStatic
        fun create(
            identity: RadixIdentity,
            universe: RadixUniverse,
            dataStoreTranslator: DataStoreTranslator,
            atomBuilderSupplier: () -> AtomBuilder
        ): RadixApplicationAPI {
            Objects.requireNonNull(identity)
            Objects.requireNonNull(universe)
            Objects.requireNonNull(atomBuilderSupplier)
            return RadixApplicationAPI(
                identity,
                universe,
                dataStoreTranslator,
                atomBuilderSupplier,
                universe.ledger
            )
        }
    }
}
