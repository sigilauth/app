import Foundation

struct PictogramEntry: Codable {
    let index: Int
    let emoji: String
    let name: String
}

struct PictogramCategory: Codable {
    let name: String
    let first_index: Int
    let count: Int
    let entries: [PictogramEntry]
}

struct PictogramPoolData: Codable {
    let version: Int
    let pool_size: Int
    let spec_ref: String
    let license: String
    let unicode_range: String
    let note: String
    let categories: [PictogramCategory]
}

public struct PictogramPool {
    private let entries: [PictogramEntry]

    public static let shared: PictogramPool = {
        guard let url = Bundle.main.url(forResource: "pictogram-pool-v1", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let poolData = try? JSONDecoder().decode(PictogramPoolData.self, from: data) else {
            fatalError("Failed to load pictogram-pool-v1.json from bundle")
        }

        let allEntries = poolData.categories.flatMap { $0.entries }.sorted { $0.index < $1.index }

        guard allEntries.count == 192 else {
            fatalError("Pictogram pool must contain exactly 192 entries, got \(allEntries.count)")
        }

        return PictogramPool(entries: allEntries)
    }()

    private init(entries: [PictogramEntry]) {
        self.entries = entries
    }

    public func entry(at index: Int) -> (emoji: String, name: String)? {
        guard index >= 0 && index < entries.count else { return nil }
        let entry = entries[index]
        return (entry.emoji, entry.name)
    }

    public var count: Int {
        return entries.count
    }
}
