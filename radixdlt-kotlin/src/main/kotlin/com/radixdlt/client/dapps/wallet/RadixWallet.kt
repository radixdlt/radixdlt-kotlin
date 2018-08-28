package com.radixdlt.client.dapps.wallet

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.assets.Amount
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import io.reactivex.Observable

/**
 * High Level API for Wallet type actions. Currently being used by the Radix Android Mobile Wallet.
 */
class RadixWallet(private val api: RadixApplicationAPI) {
    // TODO: add cancel option
    class TransferResult internal constructor(private val updates: Observable<AtomSubmissionUpdate>) {

        fun toObservable(): Observable<AtomSubmissionUpdate> {
            return updates
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
    fun getXRDTransactions(address: RadixAddress): Observable<TokenTransfer> = api.getTokenTransfers(address, Asset.TEST)

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

        val updates =
            api.transferTokens(
                api.myAddress,
                toAddress,
                Amount.subUnitsOf(amountInSubUnits, Asset.TEST),
                attachment
            ).toObservable().replay()
        updates.connect()
        return TransferResult(updates)
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
        return this.transferXRDWhenAvailable(amountInSubUnits, toAddress, null)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount with an encrypted message (readable by sender and receiver) to a specified account.
     *
     * @param amountInSubUnits The amount of TEST to transfer.
     * @param toAddress The myAddress to send to.
     * @param message The message to send as an attachment.
     * @return The result of the transaction.
     */
    fun transferXRDWhenAvailable(amountInSubUnits: Long, toAddress: RadixAddress, message: String?): TransferResult {
        val attachment: Data? = if (message != null) {
            Data.DataBuilder()
                .addReader(toAddress.publicKey)
                .addReader(api.myAddress.publicKey)
                .bytes(message.toByteArray()).build()
        } else {
            null
        }

        val updates = api.getBalance(api.myAddress, Asset.TEST)
            .filter { amount -> amount.amountInSubunits > amountInSubUnits }
            .firstOrError()
            .map {
                api.transferTokens(api.myAddress, toAddress, Amount.subUnitsOf(amountInSubUnits, Asset.TEST), attachment)
            }
            .flatMapObservable { it.toObservable() }
            .replay()

        updates.connect()

        return TransferResult(updates)
    }
}
