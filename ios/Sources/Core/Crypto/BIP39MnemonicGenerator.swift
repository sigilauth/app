import Foundation
import BIP39
import CryptoKit

public protocol BIP39MnemonicGenerator: Sendable {
    func generate() throws -> [String]
}

public enum BIP39Error: Error {
    case entropyGenerationFailed
    case mnemonicGenerationFailed
}

public final class DefaultBIP39MnemonicGenerator: BIP39MnemonicGenerator, @unchecked Sendable {
    public init() {}

    public func generate() throws -> [String] {
        var entropy = Data(count: 32)
        let result = entropy.withUnsafeMutableBytes { buffer in
            SecRandomCopyBytes(kSecRandomDefault, 32, buffer.baseAddress!)
        }

        guard result == errSecSuccess else {
            throw BIP39Error.entropyGenerationFailed
        }

        guard let mnemonic = try? Mnemonic(entropy: entropy, language: .english) else {
            throw BIP39Error.mnemonicGenerationFailed
        }

        return mnemonic.words
    }
}
