package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.TokenRef
import com.radixdlt.client.core.atoms.particles.Consumable

class AddressTokenState(val balance: Map<TokenRef, Long>, val unconsumedConsumables: Map<TokenRef, List<Consumable>>)
