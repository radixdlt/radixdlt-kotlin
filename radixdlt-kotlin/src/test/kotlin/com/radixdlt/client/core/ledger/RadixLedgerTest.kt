package com.radixdlt.client.core.ledger

import com.nhaarman.mockitokotlin2.any
import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.address.RadixUniverseConfig
import com.radixdlt.client.core.atoms.ApplicationPayloadAtom
import com.radixdlt.client.core.atoms.Atom
import com.radixdlt.client.core.atoms.AtomBuilder
import com.radixdlt.client.core.network.RadixJsonRpcClient
import com.radixdlt.client.core.network.RadixNetwork
import com.radixdlt.client.core.network.WebSocketClient
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Consumer
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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

        @Suppress("UNCHECKED_CAST")
        val observer = mock(Consumer::class.java) as Consumer<Atom>
        val client = mock(RadixJsonRpcClient::class.java)
        val network = mock(RadixNetwork::class.java)
        `when`(network.getRadixClients(any<Long>())).thenReturn(Single.just(client).toObservable())
        `when`(network.getRadixClients(any<Set<Long>>())).thenReturn(Single.just(client).toObservable())
        `when`(client.getAtoms<Atom>(any())).thenReturn(Observable.just(atom, atom))
        `when`(client.status).thenReturn(Observable.just(WebSocketClient.RadixClientStatus.OPEN))
        `when`(client.checkAPIVersion()).thenReturn(Single.just(true))

        val config = mock(RadixUniverseConfig::class.java)

        `when`(client.getUniverse()).thenReturn(Single.just(config))

        val ledger = RadixLedger(config, network)
        ledger.getAllAtoms(EUID(BigInteger.ONE)).subscribe(observer)

        verify(observer, times(1)).accept(any())
    }
}
