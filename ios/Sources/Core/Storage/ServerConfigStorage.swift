import Foundation

/// Multi-server config storage per Cascade §4 (iOS Keychain + Core Data)
public protocol ServerConfigStorage {
    /// Save server configuration after successful pairing
    func saveServerConfig(_ config: ServerConfig) async throws

    /// Retrieve all registered servers
    func allServers() async throws -> [ServerConfig]

    /// Find server by ID
    func serverConfig(for serverId: String) async throws -> ServerConfig?

    /// Delete server configuration
    func deleteServerConfig(serverId: String) async throws
}

public struct ServerConfig: Codable {
    public let serverId: String
    public let serverName: String
    public let serverURL: URL
    public let serverPublicKey: Data
    public let pictogram: [String]
    public let pictogramSpeakable: String
    public let deviceKeyLabel: String
    public let registeredAt: Date

    public init(serverId: String, serverName: String, serverURL: URL, serverPublicKey: Data, pictogram: [String], pictogramSpeakable: String, deviceKeyLabel: String, registeredAt: Date) {
        self.serverId = serverId
        self.serverName = serverName
        self.serverURL = serverURL
        self.serverPublicKey = serverPublicKey
        self.pictogram = pictogram
        self.pictogramSpeakable = pictogramSpeakable
        self.deviceKeyLabel = deviceKeyLabel
        self.registeredAt = registeredAt
    }
}

public enum StorageError: Error {
    case serverNotFound
    case saveFailed
    case deleteFailed
}
