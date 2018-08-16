package com.radixdlt.client.core.atoms

interface AtomValidator {
    @Throws(AtomValidationException::class)
    fun validate(atom: Atom)
}
