[![Build Status](https://travis-ci.com/radixdlt/radixdlt-kotlin.svg?branch=master)](https://travis-ci.com/radixdlt/radixdlt-kotlin)
[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![](https://jitpack.io/v/com.radixdlt/radixdlt-kotlin.svg)](https://jitpack.io/#com.radixdlt/radixdlt-kotlin) 
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.radixdlt%3Aradixdlt-kotlin&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.radixdlt%3Aradixdlt-kotlin) 
[![Reliability](https://sonarcloud.io/api/project_badges/measure?project=com.radixdlt%3Aradixdlt-kotlin&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=com.radixdlt%3Aradixdlt-kotlin&metric=reliability_rating) 
[![Security](https://sonarcloud.io/api/project_badges/measure?project=com.radixdlt%3Aradixdlt-kotlin&metric=security_rating)](https://sonarcloud.io/component_measures?id=com.radixdlt%3Aradixdlt-kotlin&metric=security_rating) 
[![Code Corevage](https://sonarcloud.io/api/project_badges/measure?project=com.radixdlt%3Aradixdlt-kotlin&metric=coverage)](https://sonarcloud.io/component_measures?id=com.radixdlt%3Aradixdlt-kotlin&metric=Coverage)

# radixdlt-kotlin [Deprecated]

The current master branch for this client library will connect to the old alphanet until it is decided for it to be taken down by the team.

Development of the Kotlin library for Betanet is paused until further notice.

Please use the [radixdlt-java](https://github.com/radixdlt/radixdlt-java) which is up to date with all the latest changes.

***

A Kotlin Client library for interacting with a [Radix](https://www.radixdlt.com) Distributed Ledger compatible with Kotlin/Java projects and maximising compatibility with all versions of Android.

Compatibility with lower versions of Android is achieved by avoiding the use of any Java 8 APIs e.g. Stream, Optional, Function, etc and using Kotlin built in alternatives.


## Table of contents

- [Changelog](CHANGELOG.md)
- [Features](#features)
- [Installation](#installation)
- [Getting started](#getting-started)
- [Radix dApp API](#radix-dapp-api)
- [Code examples](#code-examples)
- [Contribute](#contribute)
- [Links](#links)
- [License](#license)


## Features
* Connection to the Alphanet test network 
* Fee-less transactions for testnets
* Identity Creation
* Native token transfers
* Immutable data storage
* Instant Messaging and TEST token wallet Dapp implementation
* RXJava 2 based
* Utilizes JSON-RPC over Websockets

## Installation
Include the following gradle dependency:
### Gradle
```
repositories {
    maven { url 'https://jitpack.io' }
}
```

```
dependencies {
    implementation 'com.radixdlt:radixdlt-kotlin:0.11.6'
}
```

## Getting started

### Identities
An Identity is the user's credentials (or more technically the manager of the
public/private key pair) into the ledger, allowing a user to own tokens and send tokens
as well as decrypt data.

To create/load an identity from a file:
```kotlin
val identity: RadixIdentity = RadixIdentities.loadOrCreateEncryptedFile("filename.key", "password")
```
This will create a new file which stores the public/private key and encrypted with the given password.

### Universes
A Universe is an instance of a Radix Distributed Ledger which is defined by a genesis atom and
a dynamic set of unpermissioned nodes forming a network.

To bootstrap to the Alphanet test network:
```kotlin
RadixUniverse.bootstrap(Bootstrap.ALPHANET)
```
**Note:** No network connections will be made yet until it is required.

## Radix Dapp API
The Radix Application API is a client side API exposing high level abstractions to make
DAPP creation easier.

To initialize the API:
```kotlin
RadixUniverse.bootstrap(Bootstrap.ALPHANET) // This must be called before RadixApplicationAPI.create()
val api: RadixApplicationAPI = RadixApplicationAPI.create(identity)
```

## Code examples

### Addresses
An address is a reference to an account and allows a user to receive tokens and/or data from other users.

You can get your own address by:
```kotlin
val myAddress: RadixAddress = api.myAddress
```

Or from a base58 string:
```kotlin
val anotherAddress: RadixAddress = RadixAddress.fromString("JHB89drvftPj6zVCNjnaijURk8D8AMFw4mVja19aoBGmRXWchnJ")
```

### Storing and Retrieving Data
Immutable data can be stored on the ledger. The data can be encrypted so that only
selected identities can read the data.

To store the encrypted string `Hello` which only the user can read:
```kotlin
val myPublicKey: ECPublicKey = api.myPublicKey
val data: Data = Data.DataBuilder()
    .bytes("Hello".toByteArray())
    .addReader(myPublicKey)
    .build()
result: Result = api.storeData(data, <address>)
```

To store unencrypted data:
```kotlin
val data: Data = Data.DataBuilder()
    .bytes("Hello World".toByteArray())
    .unencrypted()
    .build()
val result: Result = api.storeData(data, <address>)
```

The returned `Result` object exposes RXJava interfaces from which you can get
notified of the status of the storage action:

```kotlin
result.toCompletable().subscribe(<on-success>, <on-error>)
```

To then read (and decrypt if necessary) all the readable data at an address:
```kotlin
val readable: Observable<UnencryptedData> = api.getReadableData(<address>)
readable.subscribe { data ->  ...  }
```

**Note:** data which is not decryptable by the user's key is simply ignored

### Sending and Retrieving Tokens
To send an amount of TEST (the testnet native token) from my account to another address:
```kotlin
val result: Result = api.sendTokens(<to-address>, Amount.of(10, Asset.TEST))
```

To retrieve all of the token transfers which have occurred in my account:
```kotlin
val transfers: Observable<TokenTransfer> = api.getMyTokenTransfers(Asset.TEST)
transfers.subscribe { tx -> ... }
```

To get a stream of the balance of TEST tokens in my account:
```kotlin
val balance: Observable<Amount> = api.getMyBalance(Asset.TEST)
balance.subscribe { bal -> ... }
```

## Contribute

Contributions are welcome, we simply ask to:

* Fork the codebase
* Make changes
* Submit a pull request for review

When contributing to this repository, we recommend discussing with the development team the change you wish to make using a [GitHub issue](https://github.com/radixdlt/radixdlt-kotlin/issues) before making changes.

Please follow our [Code of Conduct](CODE_OF_CONDUCT.md) in all your interactions with the project.

### Code style

This project uses [ktlint](https://github.com/shyiko/ktlint) via [Gradle](https://gradle.org/) dependency.  
To check code style - `gradle ktlint` (it's also bound to `gradle check`).  

## Links

| Link | Description |
| :----- | :------ |
[radixdlt.com](https://radixdlt.com/) | Radix DLT Homepage
[documentation](https://docs.radixdlt.com/) | Radix Knowledge Base
[forum](https://forum.radixdlt.com/) | Radix Technical Forum
[@radixdlt](https://twitter.com/radixdlt) | Follow Radix DLT on Twitter

## License

radixdlt-kotlin is released under the [MIT License](LICENSE).
