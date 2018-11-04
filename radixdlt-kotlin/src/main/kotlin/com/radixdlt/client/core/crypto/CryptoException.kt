package com.radixdlt.client.core.crypto

import java.security.GeneralSecurityException

open class CryptoException : GeneralSecurityException {
    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}
