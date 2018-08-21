package com.radixdlt.client.application.objects

class UnencryptedData constructor(

    // TODO: make immutable
    val data: ByteArray,

    val metaData: Map<String, Any>,

    /**
     * @return whether this bytes came from an encrypted source
     */
    val isFromEncryptedSource: Boolean
)
