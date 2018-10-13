package com.radixdlt.client.application.translate

import com.radixdlt.client.assets.Amount
import com.radixdlt.client.core.atoms.Consumable

class AddressTokenState(val balance: Amount, val unconsumedConsumables: List<Consumable>)
