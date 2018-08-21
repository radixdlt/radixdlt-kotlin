package com.radixdlt.client.core.pow

class ProofOfWorkException(pow: String, target: String) : Exception("$pow does not meet target: $target")
