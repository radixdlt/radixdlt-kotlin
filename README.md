[![Build Status](https://travis-ci.com/radixdlt/radixdlt-kotlin.svg?branch=master)](https://travis-ci.com/radixdlt/radixdlt-kotlin)
[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/radixdlt/radixdlt-kotlin/blob/master/LICENSE)
[![](https://jitpack.io/v/com.radixdlt/radixdlt-kotlin.svg)](https://jitpack.io/#com.radixdlt/radixdlt-kotlin)

# radixdlt-kotlin
A Kotlin Client library for interacting with a [Radix](https://www.radixdlt.com) Distributed Ledger compatible with Kotlin/Java projects and maximising compatibility with all versions of Android.

Compatibility with lower versions of Android is achieved by avoiding the use of any Java 8 APIs e.g. Stream, Optional, Function, etc and using Kotlin built in alternatives.  

### Code style

This project uses [ktlint](https://github.com/shyiko/ktlint) via [Gradle](https://gradle.org/) dependency.  
To check code style - `gradle ktlint` (it's also bound to `gradle check`).  

## Features
* Connection to the Alphanet test network 
* Fee-less transactions for testnets
* Identity Creation
* Native token transfers
* Immutable data storage
* Instant Messaging and TEST token wallet Dapp implementation
* RXJava 2 based
* Utilizes JSON-RPC over Websockets

# Getting Started
Include the following gradle dependency:
## Gradle
```
repositories {
    maven { url 'https://jitpack.io' }
}

```
```
dependencies {
    implementation 'com.radixdlt:radixdlt-kotlin:0.11.3'
}
```

# Identities
An Identity is the user's credentials (or more technically the manager of the
public/private key pair) into the ledger, allowing a user to own tokens and send tokens
as well as decrypt data.

To create/load an identity from a file:
```
val identity: RadixIdentity = RadixIdentities.loadOrCreateEncryptedFile("filename.key", "password")
```
This will create a new file which stores the public/private key and encrypted with the given password.

# Universes
A Universe is an instance of a Radix Distributed Ledger which is defined by a genesis atom and
a dynamic set of unpermissioned nodes forming a network.

To bootstrap to the Alphanet test network:
```
RadixUniverse.bootstrap(Bootstrap.ALPHANET)
```
NOTE: No network connections will be made yet until it is required.

# Radix Dapp API
The Radix Application API is a client side API exposing high level abstractions to make
DAPP creation easier.

To initialize the API:
```
RadixUniverse.bootstrap(Bootstrap.ALPHANET) // This must be called before RadixApplicationAPI.create()
val api: RadixApplicationAPI = RadixApplicationAPI.create(identity)
```


## Addresses
An address is a reference to an account and allows a user to receive tokens and/or data from other users.

You can get your own address by:
```
val myAddress: RadixAddress = api.myAddress
```

Or from a base58 string:
```
val anotherAddress: RadixAddress = RadixAddress.fromString("JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ")
```

## Storing and Retrieving Data
Immutable data can be stored on the ledger. The data can be encrypted so that only
selected identities can read the data.

To store the encrypted string `Hello` which only the user can read:
```
val myPublicKey: ECPublicKey = api.myPublicKey
val data: Data = Data.DataBuilder()
    .bytes("Hello".toByteArray())
    .addReader(myPublicKey)
    .build()
result: Result = api.storeData(data, <address>)
```

To store unencrypted data:
```
val data: Data = Data.DataBuilder()
    .bytes("Hello World".toByteArray())
    .unencrypted()
    .build()
val result: Result = api.storeData(data, <address>)
```

The returned `Result` object exposes RXJava interfaces from which you can get
notified of the status of the storage action:

```
result.toCompletable().subscribe(<on-success>, <on-error>)
```

To then read (and decrypt if necessary) all the readable data at an address:
```
val readable: Observable<UnencryptedData> = api.getReadableData(<address>)
readable.subscribe { data ->  ...  }
```

NOTE: data which is not decryptable by the user's key is simply ignored

## Sending and Retrieving Tokens
To send an amount of TEST (the testnet native token) from my account to another address:
```
val result: Result = api.sendTokens(<to-address>, Amount.of(10, Asset.TEST))
```

To retrieve all of the token transfers which have occurred in my account:
```
val transfers: Observable<TokenTransfer> = api.getMyTokenTransfers(Asset.TEST)
transfers.subscribe { tx -> ... }
```

To get a stream of the balance of TEST tokens in my account:
```
val balance: Observable<Amount> = api.getMyBalance(Asset.TEST)
balance.subscribe { bal -> ... }
```

