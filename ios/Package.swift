// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SigilAuth",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v16),  // iOS 16+ for App Attest
        .macOS(.v13) // For testing on Mac
    ],
    products: [
        .library(
            name: "SigilAuthCore",
            targets: ["SigilAuthCore"]
        ),
        .library(
            name: "SigilAuthUI",
            targets: ["SigilAuthUI"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/apple/swift-crypto.git", from: "3.0.0"),
        .package(url: "https://github.com/anquii/BIP39.git", from: "1.0.0"),
    ],
    targets: [
        // MARK: - C Argon2 reference implementation
        .target(
            name: "CArgon2",
            path: "Sources/CArgon2",
            sources: ["src/argon2.c", "src/core.c", "src/ref.c", "src/encoding.c", "src/thread.c", "src/blake2/blake2b.c"],
            publicHeadersPath: "include",
            cSettings: [
                .define("ARGON2_NO_THREADS")
            ]
        ),

        // MARK: - Core Layer
        .target(
            name: "SigilAuthCore",
            dependencies: [
                .product(name: "Crypto", package: "swift-crypto"),
                "CArgon2",
                .product(name: "BIP39", package: "BIP39"),
            ],
            path: "Sources/Core",
            resources: [
                .copy("Resources/pictogram-pool-v1.json")
            ]
        ),

        // MARK: - UI Layer
        .target(
            name: "SigilAuthUI",
            dependencies: ["SigilAuthCore"],
            path: "Sources/UI",
            resources: [
                .process("Resources")
            ]
        ),

        // MARK: - Tests
        .testTarget(
            name: "SigilAuthCoreTests",
            dependencies: ["SigilAuthCore"],
            path: "Tests/CoreTests"
        ),
        .testTarget(
            name: "SigilAuthUITests",
            dependencies: ["SigilAuthUI"],
            path: "Tests/UITests"
        )
    ]
)
