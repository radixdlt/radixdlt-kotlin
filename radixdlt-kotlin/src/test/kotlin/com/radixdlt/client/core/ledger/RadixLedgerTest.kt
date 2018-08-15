package com.radixdlt.client.core.ledger

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.network.RadixJsonRpcClient
import com.radixdlt.client.core.network.RadixNetwork
import com.radixdlt.client.util.any
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Consumer
import org.junit.Test
import org.mockito.Mockito.*

import java.math.BigInteger



class RadixLedgerTest {

    @Test
    @Throws(Exception::class)
    fun testFilterOutDuplicateAtoms() {
        val atom = AtomBuilder()
                .type(ApplicationPayloadAtom::class.java)
                .applicationId("Test")
                .payload("Hello")
                .addDestination(EUID(BigInteger.ONE))
                .build()
                .rawAtom

        val observer = mock(Consumer::class.java) as Consumer<ApplicationPayloadAtom>
        val client = mock(RadixJsonRpcClient::class.java)
        val network = mock(RadixNetwork::class.java)
        `when`(network.getRadixClient(any(Long::class.java))).thenReturn(Single.just(client))
        `when`(client.getAtoms<Atom>(any())).thenReturn(Observable.just(atom, atom))
        val ledger = RadixLedger(0, network)
        ledger.getAllAtoms(EUID(BigInteger.ONE), ApplicationPayloadAtom::class.java)
                .subscribe(observer)

        verify(observer, times(1)).accept(any())
    }
}