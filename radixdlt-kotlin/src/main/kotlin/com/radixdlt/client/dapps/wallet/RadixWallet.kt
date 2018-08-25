package com.radixdlt.client.dapps.wallet

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.objects.Data
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
     * with the user's address
     *
     * @return an unending Observable of balances
     */
    fun getXRDSubUnitBalance(): Observable<Long> = api.getSubUnitBalance(api.address, Asset.XRD)

    /**
     * Returns an unending stream of the latest balance of an account
     * with a specified address.
     *
     * @param address address to get balance from
     * @return an unending Observable of balances
     */
    fun getXRDSubUnitBalance(address: RadixAddress): Observable<Long> = api.getSubUnitBalance(address, Asset.XRD)

    /**
     * Returns an unending stream of transfers which have occurred and will
     * occur with the user's address
     *
     * @return an unending Observable of transfers
     */
    fun getXRDTransactions(): Observable<TokenTransfer> = api.getTokenTransfers(api.address, Asset.XRD)

    /**
     * Returns an unending stream of transfers which have occurred and will
     * occur with a specified address
     *
     * @param address address to get transfers from
     * @return an unending Observable of transfers
     */
    fun getXRDTransactions(address: RadixAddress): Observable<TokenTransfer> = api.getTokenTransfers(address, Asset.XRD)

    /**
     * Immediately try and transfer XRD from user's account to another address. If there is
     * not enough in the account TransferResult will specify so.
     *
     * @param amountInSubUnits The amount of XRD to transfer
     * @param toAddress The address to send to.
     * @return The result of the transaction.
     */
    fun transferXRD(amountInSubUnits: Long, toAddress: RadixAddress): TransferResult =
        this.transferXRD(amountInSubUnits, toAddress, null)

    /**
     * Immediately try and transfer XRD from user's account to another address with an encrypted message
     * attachment (readable by sender and receiver). If there is not enough in the account TransferResult
     * will specify so.
     *
     * @param amountInSubUnits The amount of XRD to transfer.
     * @param toAddress The address to send to.
     * @param message The message to send as an attachment.
     * @return The result of the transaction.
     */
    fun transferXRD(amountInSubUnits: Long, toAddress: RadixAddress, message: String?): TransferResult {
        val attachment: Data? = if (message != null) {
            Data.DataBuilder()
                .addReader(toAddress.publicKey)
                .addReader(api.address.publicKey)
                .bytes(message.toByteArray()).build()
        } else {
            null
        }

        val updates =
            api.transferTokens(api.address, toAddress, Asset.XRD, amountInSubUnits, attachment).toObservable().replay()
        updates.connect()
        return TransferResult(updates)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount to a specified account.
     *
     * @param amountInSubUnits The amount of XRD to transfer.
     * @param toAddress The address to send to.
     * @return The result of the transaction.
     */
    fun transferXRDWhenAvailable(amountInSubUnits: Long, toAddress: RadixAddress): TransferResult {
        return this.transferXRDWhenAvailable(amountInSubUnits, toAddress, null)
    }

    /**
     * Block indefinitely until there are enough funds in the account, then immediately transfer
     * amount with an encrypted message (readable by sender and receiver) to a specified account.
     *
     * @param amountInSubUnits The amount of XRD to transfer.
     * @param toAddress The address to send to.
     * @param message The message to send as an attachment.
     * @return The result of the transaction.
     */
    fun transferXRDWhenAvailable(amountInSubUnits: Long, toAddress: RadixAddress, message: String?): TransferResult {
        val attachment: Data? = if (message != null) {
            Data.DataBuilder()
                .addReader(toAddress.publicKey)
                .addReader(api.address.publicKey)
                .bytes(message.toByteArray()).build()
        } else {
            null
        }

        val updates = api.getSubUnitBalance(api.address, Asset.XRD)
            .filter { balance -> balance > amountInSubUnits }
            .firstOrError()
            .map { api.transferTokens(api.address, toAddress, Asset.XRD, amountInSubUnits, attachment) }
            .flatMapObservable { it.toObservable() }
            .replay()

        updates.connect()

        return TransferResult(updates)
    }
}
