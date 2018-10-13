package com.radixdlt.client.application.translate

import com.radixdlt.client.core.address.EUID
import com.radixdlt.client.core.atoms.particles.Consumable

class AddressTokenState(val balance: Map<EUID, Long>, val unconsumedConsumables: Map<EUID, List<Consumable>>)
