package com.wagmilabs.sigil.core.crypto

import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.toEntropy
import java.security.SecureRandom

interface BIP39MnemonicGenerator {
    fun generate(): List<String>
}

class DefaultBIP39MnemonicGenerator : BIP39MnemonicGenerator {
    override fun generate(): List<String> {
        val entropy = ByteArray(32).apply {
            SecureRandom().nextBytes(this)
        }

        val mnemonicCode = MnemonicCode(entropy)
        return mnemonicCode.words
    }
}
