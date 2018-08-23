package com.radixdlt.client.dapps.wallet

import com.radixdlt.client.application.RadixApplicationAPI
import com.radixdlt.client.application.actions.TokenTransfer
import com.radixdlt.client.application.objects.Data
import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.network.AtomSubmissionUpdate
import io.reactivex.Observable

class RadixWallet(private val api: RadixApplicationAPI) {

    class TransferResult internal constructor(private val updates: Observable<AtomSubmissionUpdate>) {

        fun toObservable(): Observable<AtomSubmissionUpdate> {
            return updates
        }
    }

    fun getXRDSubUnitBalance(): Observable<Long> = this.getSubUnitBalance(api.address, Asset.XRD)

    fun getXRDTransactions(): Observable<TokenTransfer> = api.getTokenTransfers(api.address, Asset.XRD)

    fun getSubUnitBalance(address: RadixAddress, tokenClass: Asset): Observable<Long> {
        return api.getSubUnitBalance(address, tokenClass)
    }

    fun getXRDSubUnitBalance(address: RadixAddress): Observable<Long> {
        return this.getSubUnitBalance(address, Asset.XRD)
    }

    fun getXRDTransactions(address: RadixAddress): Observable<TokenTransfer> {
        return api.getTokenTransfers(address, Asset.XRD)
    }

    fun transferXRD(amountInSubUnits: Long, toAddress: RadixAddress): TransferResult {
        val updates = api.transferTokens(api.address, toAddress, Asset.XRD, amountInSubUnits).toObservable().replay()
        updates.connect()
        return TransferResult(updates)
    }

    fun transferXRDWhenAvailable(amountInSubUnits: Long, toAddress: RadixAddress): TransferResult {
        return this.transferXRDWhenAvailable(amountInSubUnits, toAddress, null)
    }

    fun transferXRDWhenAvailable(amountInSubUnits: Long, toAddress: RadixAddress, message: String?): TransferResult {
        val attachment: Data? = if (message != null) {
            Data.DataBuilder()
                .addReader(toAddress.publicKey)
                .addReader(api.address.publicKey)
                .bytes(message.toByteArray()).build()
        } else null

        val updates = api.getSubUnitBalance(api.address, Asset.XRD)
            .filter { balance -> balance > amountInSubUnits }
            .firstOrError()
            .map { api.transferTokens(api.address, toAddress, Asset.XRD, amountInSubUnits, attachment) }
            .flatMapObservable(RadixApplicationAPI.Result::toObservable)
            .replay()

        updates.connect()

        return TransferResult(updates)
    }
}
