import Foundation

/// UserDefaults-backed multi-server configuration storage
/// Stores: server configs, device keypair labels, pairing state
public final class DefaultServerConfigStorage: ServerConfigStorage {

    public init() {
        self.defaults = .standard
    }

    private let defaults: UserDefaults
    private let serversKey = "sigil.servers"

    // MARK: - ServerConfigStorage Protocol

    public func saveServerConfig(_ config: ServerConfig) async throws {
        var servers = try await allServers()

        // Update or add
        if let index = servers.firstIndex(where: { $0.serverId == config.serverId }) {
            servers[index] = config
        } else {
            servers.append(config)
        }

        try await saveServers(servers)
    }

    public func allServers() async throws -> [ServerConfig] {
        guard let data = defaults.data(forKey: serversKey) else {
            return []
        }

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        do {
            return try decoder.decode([ServerConfig].self, from: data)
        } catch {
            throw StorageError.saveFailed
        }
    }

    public func serverConfig(for serverId: String) async throws -> ServerConfig? {
        let servers = try await allServers()
        return servers.first(where: { $0.serverId == serverId })
    }

    public func deleteServerConfig(serverId: String) async throws {
        var servers = try await allServers()
        servers.removeAll(where: { $0.serverId == serverId })
        try await saveServers(servers)
    }

    // MARK: - Helpers

    private func saveServers(_ servers: [ServerConfig]) async throws {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601

        do {
            let data = try encoder.encode(servers)
            defaults.set(data, forKey: serversKey)
        } catch {
            throw StorageError.saveFailed
        }
    }
}
