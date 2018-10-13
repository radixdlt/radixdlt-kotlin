package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.particles.Consumable

class AddressTokenState(val balance: Map<String, Long>, val unconsumedConsumables: Map<String, List<Consumable>>)
