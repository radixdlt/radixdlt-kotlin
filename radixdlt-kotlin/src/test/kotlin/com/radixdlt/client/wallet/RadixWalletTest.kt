package com.radixdlt.client.wallet


import com.radixdlt.client.assets.Asset
import com.radixdlt.client.core.RadixUniverse
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixAddress
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.crypto.ECPublicKey
import com.radixdlt.client.core.identity.RadixIdentity
import com.radixdlt.client.core.ledger.RadixLedger
import com.radixdlt.client.util.any
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import org.junit.Test
import org.mockito.Mockito.*

import java.util.concurrent.TimeUnit

class RadixWalletTest {

    @Test
    @Throws(Exception::class)
    fun testZeroTransactionWallet() {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixLedger::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        val address = mock(RadixAddress::class.java)
        val subscriber = mock(Consumer::class.java) as Consumer<Long>
        `when`(ledger.getAllAtoms(any(), any<Class<Atom>>())).thenReturn(Observable.empty())

        val wallet = RadixWallet(universe)
        wallet.getSubUnitBalance(address, Asset.XRD).subscribe(subscriber)
        TimeUnit.SECONDS.sleep(1)
        verify(subscriber, times(1)).accept(0L)
    }

    @Test
    @Throws(Exception::class)
    fun createTransactionWithNoFunds() {
        val universe = mock(RadixUniverse::class.java)
        val ledger = mock(RadixLedger::class.java)
        `when`(universe.ledger).thenReturn(ledger)
        val address = mock(RadixAddress::class.java)
        val toAddress = mock(RadixAddress::class.java)
        val errorHandler = mock(io.reactivex.functions.Consumer::class.java) as Consumer<Throwable>
        `when`(ledger.getAllAtoms(any<EUID>(), any<Class<Atom>>())).thenReturn(Observable.empty())
        val radixIdentity = mock(RadixIdentity::class.java)
        `when`(universe.getAddressFrom(any<ECPublicKey>())).thenReturn(address)

        val wallet = RadixWallet(universe)
        wallet.transferXRD(10, radixIdentity, toAddress)
                .subscribe(
                        { },
                        { throwable -> errorHandler.accept(throwable.cause) }
                )

        verify(errorHandler, times(1)).accept(InsufficientFundsException(Asset.XRD, 0, 10))
    }
}
