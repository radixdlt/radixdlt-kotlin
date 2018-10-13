package com.radixdlt.client.application.translate

import com.radixdlt.client.application.objects.Amount
import com.radixdlt.client.core.atoms.particles.Consumable

class AddressTokenState(val balance: Amount, val unconsumedConsumables: List<Consumable>)
