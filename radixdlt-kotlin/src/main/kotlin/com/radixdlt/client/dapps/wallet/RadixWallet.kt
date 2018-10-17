package com.radixdlt.client.dapps.wallet

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.application.objects.TokenTransfer
import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.annotations.NonNull
import io.reactivex.annotations.Nullable
import java.math.BigDecimal

/**
 * High Level API for Wallet type actions. Currently being used by the Radix Android Mobile Wallet.
 */
class RadixWallet(private val api: RadixApplicationAPI) {
    // TODO: add cancel option
    class SendResult {
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
    fun getBalance(): Observable<Amount> = api.getMyBalance(Asset.TEST)

    /**
     * Returns an unending stream of the latest balance of an account
     * with a specified myAddress.
     *
     * @param address myAddress to get balance from
     * @return an unending Observable of balances
     */
    fun getBalance(address: RadixAddress): Observable<Amount> = api.getBalance(address, Asset.TEST)

    /**
     * Returns an unending stream of transfers which have occurred and will
     * occur with the user's myAddress
     *
     * @return an unending Observable of transfers
     */
    fun getTransactions(): Observable<TokenTransfer> = api.getTokenTransfers(api.myAddress, Asset.TEST)

    /**
     * Returns an unending stream of transfers which have occurred and will
     * occur with a specified myAddress
     *
     * @param address myAddress to get transfers from
     * @return an unending Observable of transfers
     */
    fun getTransactions(address: RadixAddress): Observable<TokenTransfer> = api.getTokenTransfers(address, Asset.TEST)

    /**
     * Immediately try and transfer TEST from user's account to another myAddress. If there is
     * not enough in the account TransferResult will specify so.
     *
     * @param amount The amount of TEST to transfer
     * @param toAddress The myAddress to send to.
     * @return The result of the transaction.
     */
    fun send(amount: BigDecimal, toAddress: RadixAddress): SendResult = this.send(amount, null, toAddress)

    /**
     * Immediately try and transfer TEST from user's account to another myAddress with an encrypted message
     * attachment (readable by sender and receiver). If there is not enough in the account TransferResult
     * will specify so.
     *
     * @param amount The amount of TEST to transfer.
     * @param message The message to send as an attachment.
     * @param toAddress The myAddress to send to.
     * @return The result of the transaction.
     */
    fun send(amount: BigDecimal, message: String?, toAddress: RadixAddress): SendResult {
        val attachment: Data? = if (message != null) {
            Data.DataBuilder()
                .addReader(toAddress.publicKey)
                .addReader(api.myAddress.publicKey)
                .bytes(message.toByteArray()).build()
        } else {
            null
        }

        val result = api.sendTokens(toAddress, Amount.of(amount, Asset.TEST), attachment)
        return SendResult(result)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount to a specified account.
     *
     * @param amount The amount of TEST to transfer.
     * @param toAddress The myAddress to send to.
     * @return The result of the transaction.
     */
    fun sendWhenAvailable(amount: BigDecimal, toAddress: RadixAddress): SendResult {
        return this.sendWhenAvailable(amount, null, toAddress, null)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount with an encrypted message (readable by sender and receiver) to a specified account.
     *
     * @param amount The amount of TEST to transfer.
     * @param message The message to send as an attachment.
     * @param toAddress The address to send to.
     * @return The result of the transaction.
     */
    fun sendWhenAvailable(
        amount: BigDecimal,
        @Nullable message: String,
        @NonNull toAddress: RadixAddress
    ): SendResult {
        return sendWhenAvailable(amount, message, toAddress, null)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount with an encrypted message (readable by sender and receiver) to a specified account.
     *
     * @param amount The amount of TEST to transfer.
     * @param message The message to send as an attachment.
     * @param toAddress The myAddress to send to.
     * @param unique The unique id for this transaction.
     * @return The result of the transaction.
     */
    fun sendWhenAvailable(
        amount: BigDecimal,
        message: String?,
        toAddress: RadixAddress,
        unique: String?
    ): SendResult {
        val attachment: Data? = if (message != null) {
            Data.DataBuilder()
                .addReader(toAddress.publicKey)
                .addReader(api.myPublicKey)
                .bytes(message.toByteArray()).build()
        } else {
            null
        }

        val uniqueBytes = unique?.toByteArray()

        val amountToSend = Amount.of(amount, Asset.TEST)

        val result = api.getMyBalance(Asset.TEST)
            .filter(amountToSend::lte)
            .firstOrError()
            .map { api.sendTokens(toAddress, amountToSend, attachment, uniqueBytes) }
            .cache()
        result.subscribe()

        return SendResult(result)
    }
}
