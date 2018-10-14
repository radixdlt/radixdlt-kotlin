package com.radixdlt.client.application.translate

import com.radixdlt.client.core.atoms.TokenReference
import com.radixdlt.client.core.atoms.particles.Consumable

class AddressTokenState(val balance: Map<TokenReference, Long>, val unconsumedConsumables: Map<TokenReference, List<Consumable>>)
