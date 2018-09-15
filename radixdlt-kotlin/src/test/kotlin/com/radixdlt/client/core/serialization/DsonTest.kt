package com.radixdlt.client.core.serialization

import com.radixdlt.client.core.address.RadixUniverseConfig
import org.bouncycastle.util.encoders.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class DsonTest {
    @Test
    fun fromDsonTest() {
        val dson = Dson.instance
        val jsonElement = dson.parse(Base64.decode(
                            "BbfPB2NyZWF0b3IEIQN4Wpwln96ZkeRPovsLVlnypXgawzkHbi2/73BSjkrfaAtkZXNjcmlwdGlvbgMeVGhlIFJhZGl4IGRldmVsb3BtZW50IFVuaX"
                    +		"ZlcnNlB2dlbmVzaXMGtpQFqQ8GYWN0aW9uAwVTVE9SRQVhc3NldAWnbg5jbGFzc2lmaWNhdGlvbgMJY29tbW9kaXR5C2Rlc2NyaXB0aW9uAwlSYWRp"
                    +		"eCBQT1cEaWNvbgSmoYlQTkcNChoKAAAADUlIRFIAAAAgAAAAIAgGAAAAc3p69AAAAAlwSFlzAAAuIwAALiMBeKU/dgAAAB1pVFh0U29mdHdhcmUAAA"
                    +		"AAAEFkb2JlIEltYWdlUmVhZHkGrQKXAAAGKklEQVRYha1Xa1BUVRz/nXvPtrCYpuDKspspT1F648oq0IqATqnN+M4XMdPDshk1pyanD02PyQ9liWPa"
                    +		"YImMMonVjNo0iDx2g03evYRgSQSVx+Ly0HJhl73nnj7gGrirPOI3cz/ce849/9//cf4PwjnHSJAkSaxraIwuNJUuMVvKjLUN1hjbdXuw0+kKAAB/P6"
                    +		"UjWK22zYuOvGiMN5iTjQlF8+ZENlBK2Uhnk/sRkBgTyyprFmTl5KYVmkqXtts6dUySRBACEDJ8M+cA5xAplUKCZ7SmLE7MT9+0Ltugf7pCFEV5zASu"
                    +		"trbp9h3M3JmT+/3W7q6e6RAIIAgjKTQIWQZkjsCgQPvmDauz39z+yv6ZupA2n3s5515PaVll3IKk5RZMDuGYouV4SDe+Z4qWY3IIj0teUWIpr9L7ku"
                    +		"VlgbxCU/L23e8eam5uiYAojk7jkcAYwkJnNx7+fO9rKYsTi4cuDSNgKa+K2/rqjuPNzS3hEyZ8CInQ0NmNxzMztizUx1Z6Ebja2qZdm7btVGVVzcIJ"
                    +		"Fz6ERFzcfMu3WV+u12k17QBAAcAtSXTfwcxdldW/GP6XcJkD8BHUnlsjiiivqF6074vMnZ988O4eSikjnHNYyqsMz7+Qfran90aQ1/UaJYhCARI0FU"
                    +		"QQhnMgBNzhgHzj78F3zhE4bar9zDdZKxYtiK2gbrebHj1xMr2nqycIdPza+6etg9/ald75gYpwnTkHx/7MO5botndNz8rJfVH/9BM1tL7xUlSBqXTp"
                    +		"qO/43ZBliDN1UK5eDkEzA3J3z2Ae8EAQvEkJAs4Xlyyz/tUUQQtMJckdtk4thPGZHhx4ICke4sNauMtrcOvjDMA9MGQDAb/lGE5CENDeYdMVmkuXUF"
                    +		"NpmZFJkjguC3AOIXAqlMuSAMbg+iEfzHoJEO86y0fqZpJETaUXjLSuwRoz3sCDLENhiAWdGwmpsQkDlkqAit4m9wVCUFtvfZTaOu0anz/cXSO8ig9A"
                    +		"VCoon0sGUSgwkG+GbO8afb0gBLZOu4Y6XS6Vr0UyeRLI7ZzAB9zefpQZ6GNzoYh9AqytA67CktEJHoL+fqeKen3lHGRSACa99xZo2CyAc8g9vXB8eh"
                    +		"hSbcN//lUooHx2CYQpD6L/bD7YlWuj1/4/YaB+fso+T2NxB4RACJwGIVgNCARiVBgCdryMf975EHLvzcG6Hz4bDyQaIPfehCuvCJCYd/CNAJXK30GD"
                    +		"1eqOlitXw++YlxDwvn7cev8TEKUSwtQpCHj7DSgW6eG3ZR36Dn4NMAZlqhGiZgZc+SZIdQ1jFg7OEaxWd9CY6KiLLS1Xwof5lzGwppbBlMplkAAVJn"
                    +		"20B/4bV0G6WA+pth7KlGfAnU64fiwA73dizDWEc8TMjaqlxgSDOa+geCWT5eEn3PGnAFexBfTkafi/tAmq19PhLquCGPoIpN/rMFD56zh8D4gKKhnj"
                    +		"DSaaYkwo3B88o7W1tf2Re2ZDxtCfnQs6L3Lw3ofNAgC48orAe2+O3fyyDK1O25psTCii0ZERjamLE/OPZue8AsH7UgAACIHc1QPHga8wedZMCFoNWF"
                    +		"MzXOYLo0s6XgQ4UpMS8+ZEhF2iCgWV0jevzzr9Y/6qnt7ee5djUYD0x5/o+zIbqu3pcJ09D7ndhjHXEM4ROD3Inr5p/TFKqUQBIC72yeotG1Yfyzh0"
                    +		"ZDfIfVQiBM4z5+Cu/g3y9a6xCfYcwTlP27jmqP6px2s87wCAa23tIWtf3JZbUVEdP2JEy7LPAjMiGIPBoC85lXV4gy5E0zGMAABcqKjWb3l1x4nLl5"
                    +		"snriMeIjwsLNR6PPPAZsP8p6o9n73a8gJTSdJru/YcbrrcHDmRbXl4WKj10Od7t6UYE8zD1nwNCz+XV+kNKStLJmowWZj6/E8XKqvnj2ow8eBaW3vI"
                    +		"Z18c2Xn85Hdp3fZu9XhGsyB1UOfWDWuO7Xr95QyddtDnd+O+wyljTCir+kV/LOdUWoGpZGlbh+1hJkkUuB2Anhjk/HYnzCEqqKTTaK6lJiWeS9u4Lj"
                    +		"su9smqcQ2nQyExJtRb/5pTZC5NMlnKjHX11hjbdbumr98ZAAAqfz9HsFrdERMdddGYYDAnPxNfHB0VYb2fYA/+BeLxAk0mNtlqAAAAAElFTkSuQmCC"
                    +		"A2lzbwMDUE9XBWxhYmVsAw1Qcm9vZiBvZiBXb3JrDW1heGltdW1fdW5pdHMCCAAAAAAAAAAACnNlcmlhbGl6ZXICCAAAAAADuvLQCHNldHRpbmdzAg"
                    +		"gAAAAAAAAQAAlzdWJfdW5pdHMCCAAAAAAAAAAABHR5cGUDCUNPTU1PRElUWQd2ZXJzaW9uAggAAAAAAAAAZAxkZXN0aW5hdGlvbnMGEgcQVqurOHBY"
                    +		"XwTQFdVa32ALxwJpZAcQIpydeQV2HSTqn6/P9k09SQZvd25lcnMGUwVRBnB1YmxpYwQhA3hanCWf3pmR5E+i+wtWWfKleBrDOQduLb/vcFKOSt9oCn"
                    +		"NlcmlhbGl6ZXICCAAAAAAgne87B3ZlcnNpb24CCAAAAAAAAABkCnNlcmlhbGl6ZXICCAAAAAAAHtFRCnNpZ25hdHVyZXMFkyA1NmFiYWIzODcwNTg1"
                    +		"ZjA0ZDAxNWQ1NWFkZjYwMGJjNwVwAXIEIDEGhJhAy62NzKPkq2QEUZYvFBDlcB+zTA6iwET0uWflAXMEIQD+pMvOsmXw1BRNLicUde6IfMCcWd415O"
                    +		"qKVbJo67rYCApzZXJpYWxpemVyAgj/////5hWomAd2ZXJzaW9uAggAAAAAAAAAZAp0aW1lc3RhbXBzBSQHZGVmYXVsdAIIAAABWocqmAAHZXhwaXJl"
                    +		"cwIIf/////////8HdmVyc2lvbgIIAAAAAAAAAGQFqUwGYWN0aW9uAwVTVE9SRQVhc3NldAWnqw5jbGFzc2lmaWNhdGlvbgMKY3VycmVuY2llcwtkZX"
                    +		"NjcmlwdGlvbgMZUmFkaXggVGVzdCBjdXJyZW5jeSBhc3NldARpY29uBKahiVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAACXBIWXMA"
                    +		"AC4jAAAuIwF4pT92AAAAHWlUWHRTb2Z0d2FyZQAAAAAAQWRvYmUgSW1hZ2VSZWFkeQatApcAAAYqSURBVFiFrVdrUFRVHP+de8+2sJim4MqymylPUX"
                    +		"rjyirQioBOqc34zhcx08OyGTWnJqcPTY/JD2WJY9pgiYwyidWM2jSIPHaDTd69hGBJBJXH4vLQcmGXveeePuAauKs84jdzP9x7zj3/3/9x/g/COcdI"
                    +		"kCRJrGtojC40lS4xW8qMtQ3WGNt1e7DT6QoAAH8/pSNYrbbNi468aIw3mJONCUXz5kQ2UErZSGeT+xGQGBPLKmsWZOXkphWaSpe22zp1TJJEEAIQMn"
                    +		"wz5wDnECmVQoJntKYsTsxP37Qu26B/ukIURXnMBK62tun2HczcmZP7/dburp7pEAggCCMpNAhZBmSOwKBA++YNq7Pf3P7K/pm6kDafeznnXk9pWWXc"
                    +		"gqTlFkwO4Zii5XhIN75nipZjcgiPS15RYimv0vuS5WWBvEJT8vbd7x5qbm6JgCiOTuORwBjCQmc3Hv5872spixOLhy4NI2Apr4rb+uqO483NLeETJn"
                    +		"wIidDQ2Y3HMzO2LNTHVnoRuNrapl2btu1UZVXNwgkXPoREXNx8y7dZX67XaTXtAEABwC1JdN/BzF2V1b8Y/pdwmQPwEdSeWyOKKK+oXrTvi8ydn3zw"
                    +		"7h5KKSOcc1jKqwzPv5B+tqf3RpDX9RoliEIBEjQVRBCGcyAE3OGAfOPvwXfOEThtqv3MN1krFi2IraBut5sePXEyvaerJwh0/Nr7p62D39qV3vmBin"
                    +		"CdOQfH/sw7lui2d03Pysl9Uf/0EzW0vvFSVIGpdOmo7/jdkGWIM3VQrl4OQTMDcnfPYB7wQBC8SQkCzheXLLP+1RRBC0wlyR22Ti2E8ZkeHHggKR7i"
                    +		"w1q4y2tw6+MMwD0wZAMBv+UYTkIQ0N5h0xWaS5dQU2mZkUmSOC4LcA4hcCqUy5IAxuD6IR/MegkQ7zrLR+pmkkRNpReMtK7BGjPewIMsQ2GIBZ0bCa"
                    +		"mxCQOWSoCK3ib3BUJQW299lNo67RqfP9xdI7yKD0BUKiifSwZRKDCQb4Zs7xp9vSAEtk67hjpdLpWvRTJ5EsjtnMAH3N5+lBnoY3OhiH0CrK0DrsKS"
                    +		"0Qkegv5+p4p6feUcZFIAJr33FmjYLIBzyD29cHx6GFJtw3/+VSigfHYJhCkPov9sPtiVa6PX/j9hoH5+yj5PY3EHhEAInAYhWA0IBGJUGAJ2vIx/3v"
                    +		"kQcu/NwbofPhsPJBog996EK68IkJh38I0AlcrfQYPV6o6WK1fD75iXEPC+ftx6/xMQpRLC1CkIePsNKBbp4bdlHfoOfg0wBmWqEaJmBlz5Jkh1DWMW"
                    +		"Ds4RrFZ30JjoqIstLVfCh/mXMbCmlsGUymWQABUmfbQH/htXQbpYD6m2HsqUZ8CdTrh+LADvd2LMNYRzxMyNqqXGBIM5r6B4JZPl4Sfc8acAV7EF9O"
                    +		"Rp+L+0CarX0+Euq4IY+gik3+swUPnrOHwPiAoqGeMNJppiTCjcHzyjtbW1/ZF7ZkPG0J+dCzovcvDeh80CALjyisB7b47d/LIMrU7bmmxMKKLRkRGN"
                    +		"qYsT849m57wCwftSAAAIgdzVA8eBrzB51kwIWg1YUzNc5gujSzpeBDhSkxLz5kSEXaIKBZXSN6/POv1j/qqe3t57l2NRgPTHn+j7Mhuq7elwnT0Pud"
                    +		"2GMdcQzhE4Pcievmn9MUqpRAEgLvbJ6i0bVh/LOHRkN8h9VCIEzjPn4K7+DfL1rrEJ9hzBOU/buOao/qnHazzvAIBrbe0ha1/clltRUR0/YkTLss8C"
                    +		"MyIYg8GgLzmVdXiDLkTTMYwAAFyoqNZveXXHicuXmyeuIx4iPCws1Ho888Bmw/ynqj2fvdryAlNJ0mu79hxuutwcOZFteXhYqPXQ53u3pRgTzMPWfA"
                    +		"0LP5dX6Q0pK0smajBZmPr8Txcqq+ePajDx4Fpbe8hnXxzZefzkd2nd9m71eEazIHVQ59YNa47tev3lDJ120Od3477DKWNMKKv6RX8s51RagalkaVuH"
                    +		"7WEmSRS4HYCeGOT8difMISqopNNorqUmJZ5L27guOy72yapxDadDITEm1Fv/mlNkLk0yWcqMdfXWGNt1u6av3xkAACp/P0ewWt0REx110ZhgMCc/E1"
                    +		"8cHRVhvZ9gD/4F4vECTSY22WoAAAAASUVORK5CYIIDaXNvAwRURVNUBWxhYmVsAwlUZXN0IFJhZHMNbWF4aW11bV91bml0cwIIAAAAAAAAAAAGc2Ny"
                    +		"eXB0BScKc2VyaWFsaXplcgIIAAAAACC6bCgHdmVyc2lvbgIIAAAAAAAAAGQKc2VyaWFsaXplcgIIAAAAAAO68tAIc2V0dGluZ3MCCAAAAAAAAFADCX"
                    +		"N1Yl91bml0cwIIAAAAAAABhqAEdHlwZQMIQ1VSUkVOQ1kHdmVyc2lvbgIIAAAAAAAAAGQMZGVzdGluYXRpb25zBhIHEFarqzhwWF8E0BXVWt9gC8cC"
                    +		"aWQHENe9NL/kShjSqnVaNE/j5rAGb3duZXJzBlMFUQZwdWJsaWMEIQN4Wpwln96ZkeRPovsLVlnypXgawzkHbi2/73BSjkrfaApzZXJpYWxpemVyAg"
                    +		"gAAAAAIJ3vOwd2ZXJzaW9uAggAAAAAAAAAZApzZXJpYWxpemVyAggAAAAAAB7RUQpzaWduYXR1cmVzBZMgNTZhYmFiMzg3MDU4NWYwNGQwMTVkNTVh"
                    +		"ZGY2MDBiYzcFcAFyBCB83Gha2MjwYdhb0dVO7WvHodHjrVgqNqEiDfFcGkCdswFzBCEA918r4iWbK6baqOvNOHtEFntqvtpAWSbb1GpRrnH71ToKc2"
                    +		"VyaWFsaXplcgII/////+YVqJgHdmVyc2lvbgIIAAAAAAAAAGQKdGltZXN0YW1wcwUkB2RlZmF1bHQCCAAAAVqHKpgAB2V4cGlyZXMCCH//////////"
                    +		"B3ZlcnNpb24CCAAAAAAAAABkBaQwBmFjdGlvbgMFU1RPUkUMZGF0YVBhcnRpY2xlBUQFYnl0ZXMEFVJhZGl4Li4uLkp1c3QgSW1hZ2luZQpzZXJpYW"
                    +		"xpemVyAggAAAAAHDz8MAd2ZXJzaW9uAggAAAAAAAAAZAxkZXN0aW5hdGlvbnMGEgcQVqurOHBYXwTQFdVa32ALxwlwYXJ0aWNsZXMGoOUFoOIIYXNz"
                    +		"ZXRfaWQHENe9NL/kShjSqnVaNE/j5rAMZGVzdGluYXRpb25zBhIHEFarqzhwWF8E0BXVWt9gC8cFbm9uY2UCCAACjbhK44SABm93bmVycwZTBVEGcH"
                    +		"VibGljBCEDeFqcJZ/emZHkT6L7C1ZZ8qV4GsM5B24tv+9wUo5K32gKc2VyaWFsaXplcgIIAAAAACCd7zsHdmVyc2lvbgIIAAAAAAAAAGQIcXVhbnRp"
                    +		"dHkCCAAAWvMQekAACnNlcmlhbGl6ZXICCAAAAABqOyWHB3ZlcnNpb24CCAAAAAAAAABkCnNlcmlhbGl6ZXICCAAAAAAAHtFRCnNpZ25hdHVyZXMFky"
                    +		"A1NmFiYWIzODcwNTg1ZjA0ZDAxNWQ1NWFkZjYwMGJjNwVwAXIEIB5Blxt878bLjf5FAKnSAVQLh+Tus5GTTYKbyaCDlt8lAXMEIQDZmRhR6O4l/hT0"
                    +		"hSJTPVaIyL/pDbZwyxr3BYTjiVKuwQpzZXJpYWxpemVyAgj/////5hWomAd2ZXJzaW9uAggAAAAAAAAAZA50ZW1wb3JhbF9wcm9vZgWhxAdhdG9tX2"
                    +		"lkBxD6/ikythfpiB+pVNYcpmNICnNlcmlhbGl6ZXICCAAAAABxjp9CB3ZlcnNpb24CCAAAAAAAAABkCHZlcnRpY2VzBqF3BaF0BWNsb2NrAggAAAAA"
                    +		"AAAAAApjb21taXRtZW50CCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAVvd25lcgVRBnB1YmxpYwQhAiDi9s7Y2EOIS7Jt7WJ09KempP"
                    +		"MII3hb3HeBV/MaA/qmCnNlcmlhbGl6ZXICCAAAAAAgne87B3ZlcnNpb24CCAAAAAAAAABkCHByZXZpb3VzBxAAAAAAAAAAAAAAAAAAAAAACnNlcmlh"
                    +		"bGl6ZXICCP/////JzJtGCXNpZ25hdHVyZQVxAXIEIQDrWLNXbOntBj46deA9JnB3rkT+H8hisbKnsfaRszeKrQFzBCEA8k5IGik12IoO9JP17NmHL9"
                    +		"voqDYn2aRbQAw/QiAy1GAKc2VyaWFsaXplcgII/////+YVqJgHdmVyc2lvbgIIAAAAAAAAAGQKdGltZXN0YW1wcwUSB2RlZmF1bHQCCAAAAWWqVwqE"
                    +		"B3ZlcnNpb24CCAAAAAAAAABkCnRpbWVzdGFtcHMFEgdkZWZhdWx0AggAAAFahyqYAAd2ZXJzaW9uAggAAAAAAAAAZAVtYWdpYwIIAAAAAAPNgAIEbm"
                    +		"FtZQMMUmFkaXggRGV2bmV0BHBvcnQCCAAAAAAAAHUwCnNlcmlhbGl6ZXICCAAAAAAdWDpFC3NpZ25hdHVyZS5yBCAuvkVsH5BzzFK/cVmtnAthO8+6"
                    +		"2AxisaUbCoflCF/WkgtzaWduYXR1cmUucwQhAOlX2i/bIYIMX5OIF7FUIb6+2X7TzgJVm7PEbY5V8TaiCXRpbWVzdGFtcAIIAAABWocqmAAEdHlwZQ"
                    +		"IIAAAAAAAAAAIHdmVyc2lvbgIIAAAAAAAAAGQ=")
        )
        val universeFromDson = RadixJson.gson.fromJson(jsonElement, RadixUniverseConfig::class.java)
        assertEquals(63799298, universeFromDson.magic.toLong())
        assertEquals(3, universeFromDson.genesis.size.toLong())
    }
}
