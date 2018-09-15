package com.radixdlt.client.core.atoms

import java.util.TreeMap

/**
 * Distinct type for metadata maps, as these need to be serialized
 * and deserialized differently.
 */
class MetadataMap : TreeMap<String, String>()
