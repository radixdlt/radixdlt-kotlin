package com.radixdlt.client.assets

import java.math.BigDecimal

/**
 * Class mainly for formatting amounts in error messages and other English text.
 */
class AssetAmount(private val asset: Asset, private val amountInSubunits: Long) {

    override fun toString(): String {
        return "${formattedAmount()} ${asset.iso}"
    }

    private fun formattedAmount(): String {
        val remainder = amountInSubunits % asset.subUnits

        if (remainder == 0L) {
            // Whole number
            return (amountInSubunits / asset.subUnits).toString()
        }

        if (isPowerOfTen(asset.subUnits)) {
            // Decimal format
            return BigDecimal.valueOf(amountInSubunits).divide(BigDecimal.valueOf(asset.subUnits.toLong())).toString()
        }

        // Fraction format
        val quotient = amountInSubunits / asset.subUnits
        val fraction = remainder.toString() + "/" + asset.subUnits
        return if (quotient == 0L) {
            fraction
        } else quotient.toString() + " and " + fraction
    }

    private fun isPowerOfTen(value: Int): Boolean {
        var valueCalculation = value
        while (valueCalculation > 9 && valueCalculation % 10 == 0) {
            valueCalculation /= 10
        }
        return valueCalculation == 1
    }
}
