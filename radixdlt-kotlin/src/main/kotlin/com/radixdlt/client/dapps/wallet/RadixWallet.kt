package com.radixdlt.client.dapps.wallet

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.annotations.NonNull
import io.reactivex.annotations.Nullable

/**
 * High Level API for Wallet type actions. Currently being used by the Radix Android Mobile Wallet.
 */
class RadixWallet(private val api: RadixApplicationAPI) {
    // TODO: add cancel option
    class TransferResult {
        private val result: Single<RadixApplicationAPI.Result>

        internal constructor(result: RadixApplicationAPI.Result) {
            this.result = Single.just(result)
        }

        internal constructor(result: Single<RadixApplicationAPI.Result>) {
            this.result = result
        }

        fun toObservable(): Observable<AtomSubmissionUpdate> {
            return result.flatMapObservable(RadixApplicationAPI.Result::toObservable)
        }

        fun toCompletable(): Completable {
            return result.flatMapCompletable(RadixApplicationAPI.Result::toCompletable)
        }
    }

    /**
     * Returns an unending stream of the latest balance of an account
     * with the user's myAddress
     *
     * @return an unending Observable of balances
     */
    fun getXRDBalance(): Observable<Amount> = api.getBalance(api.myAddress, Asset.TEST)

    /**
     * Returns an unending stream of the latest balance of an account
     * with a specified myAddress.
     *
     * @param address myAddress to get balance from
     * @return an unending Observable of balances
     */
    fun getXRDBalance(address: RadixAddress): Observable<Amount> = api.getBalance(address, Asset.TEST)

    /**
     * Returns an unending stream of transfers which have occurred and will
     * occur with the user's myAddress
     *
     * @return an unending Observable of transfers
     */
    fun getXRDTransactions(): Observable<TokenTransfer> = api.getTokenTransfers(api.myAddress, Asset.TEST)

    /**
     * Returns an unending stream of transfers which have occurred and will
     * occur with a specified myAddress
     *
     * @param address myAddress to get transfers from
     * @return an unending Observable of transfers
     */
    fun getXRDTransactions(address: RadixAddress): Observable<TokenTransfer> =
        api.getTokenTransfers(address, Asset.TEST)

    /**
     * Immediately try and transfer TEST from user's account to another myAddress. If there is
     * not enough in the account TransferResult will specify so.
     *
     * @param amountInSubUnits The amount of TEST to transfer
     * @param toAddress The myAddress to send to.
     * @return The result of the transaction.
     */
    fun transferXRD(amountInSubUnits: Long, toAddress: RadixAddress): TransferResult =
        this.transferXRD(amountInSubUnits, toAddress, null)

    /**
     * Immediately try and transfer TEST from user's account to another myAddress with an encrypted message
     * attachment (readable by sender and receiver). If there is not enough in the account TransferResult
     * will specify so.
     *
     * @param amountInSubUnits The amount of TEST to transfer.
     * @param toAddress The myAddress to send to.
     * @param message The message to send as an attachment.
     * @return The result of the transaction.
     */
    fun transferXRD(amountInSubUnits: Long, toAddress: RadixAddress, message: String?): TransferResult {
        val attachment: Data? = if (message != null) {
            Data.DataBuilder()
                .addReader(toAddress.publicKey)
                .addReader(api.myAddress.publicKey)
                .bytes(message.toByteArray()).build()
        } else {
            null
        }

        val result = api.sendTokens(toAddress, Amount.subUnitsOf(amountInSubUnits, Asset.TEST), attachment)
        return TransferResult(result)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount to a specified account.
     *
     * @param amountInSubUnits The amount of TEST to transfer.
     * @param toAddress The myAddress to send to.
     * @return The result of the transaction.
     */
    fun transferXRDWhenAvailable(amountInSubUnits: Long, toAddress: RadixAddress): TransferResult {
        return this.transferXRDWhenAvailable(amountInSubUnits, toAddress, null, null)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount with an encrypted message (readable by sender and receiver) to a specified account.
     *
     * @param amountInSubUnits The amount of TEST to transfer.
     * @param toAddress The address to send to.
     * @param message The message to send as an attachment.
     * @return The result of the transaction.
     */
    fun transferXRDWhenAvailable(
        amountInSubUnits: Long,
        @NonNull toAddress: RadixAddress,
        @Nullable message: String
    ): TransferResult {
        return transferXRDWhenAvailable(amountInSubUnits, toAddress, message, null)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount with an encrypted message (readable by sender and receiver) to a specified account.
     *
     * @param amountInSubUnits The amount of TEST to transfer.
     * @param toAddress The myAddress to send to.
     * @param message The message to send as an attachment.
     * @param unique The unique id for this transaction.
     * @return The result of the transaction.
     */
    fun transferXRDWhenAvailable(
        amountInSubUnits: Long,
        toAddress: RadixAddress,
        message: String?,
        unique: String?
    ): TransferResult {
        val attachment: Data? = if (message != null) {
            Data.DataBuilder()
                .addReader(toAddress.publicKey)
                .addReader(api.myPublicKey)
                .bytes(message.toByteArray()).build()
        } else {
            null
        }

        val uniqueBytes = unique?.toByteArray()

        val result = api.getMyBalance(Asset.TEST)
            .filter { amount -> amount.amountInSubunits >= amountInSubUnits }
            .firstOrError()
            .map {
                api.sendTokens(
                    toAddress,
                    Amount.subUnitsOf(amountInSubUnits, Asset.TEST),
                    attachment,
                    uniqueBytes
                )
            }
            .cache()
        result.subscribe()

        return TransferResult(result)
    }
}
