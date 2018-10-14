package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.Token
import com.radixdlt.client.core.atoms.particles.Consumable

class AddressTokenState(val balance: Map<Token, Long>, val unconsumedConsumables: Map<Token, List<Consumable>>)
