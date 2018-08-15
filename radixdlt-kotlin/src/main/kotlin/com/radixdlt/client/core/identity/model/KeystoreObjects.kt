package com.radixdlt.client.core.identity.model

class Keystore(var crypto: Crypto? = null, var id: String = "")
class Cipherparams(var iv: String = "")
class Crypto (var cipher: String = "", var cipherparams: Cipherparams? = null, var ciphertext: String = "", var pbkdfparams: Pbkdfparams? = null, var mac: String = "")
class Pbkdfparams (var iterations: Int = 0, var keylen: Int = 0, var digest: String = "", var salt: String = "")