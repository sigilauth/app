import SwiftUI
import SigilAuthUI
import SigilAuthCore
import FirebaseCore
import FirebaseCrashlytics

@main
struct SigilAuthApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}

class AppState: ObservableObject {
    @Published var isOnboardingComplete: Bool = false
    @Published var pairedServers: [ServerConfig] = []
    @Published var trustedServers: [TrustedServer] = []

    private let trustStorage = KeychainTrustStorage()

    init() {
        FirebaseApp.configure()
        loadServerConfiguration()
        loadTrustedServers()
        registerForRemoteNotifications()
    }

    private func loadServerConfiguration() {
        Task {
            let storage = DefaultServerConfigStorage()
            pairedServers = (try? await storage.allServers()) ?? []
            isOnboardingComplete = !pairedServers.isEmpty
        }
    }

    private func loadTrustedServers() {
        Task {
            do {
                trustedServers = try trustStorage.loadAllTrustedServers()
            } catch {
                print("Failed to load trusted servers: \(error)")
                trustedServers = []
            }
        }
    }

    func getTrust(for fingerprint: String) -> TrustedServer? {
        trustedServers.first { $0.serverFingerprint == fingerprint }
    }

    private func registerForRemoteNotifications() {
        Task { @MainActor in
            let center = UNUserNotificationCenter.current()
            let granted = try? await center.requestAuthorization(options: [.alert, .sound, .badge])

            if granted == true {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }
}

struct ContentView: View {
    @EnvironmentObject var appState: AppState

    var body: some View {
        NavigationStack {
            if appState.isOnboardingComplete {
                ServerListView()
            } else {
                PairingView()
            }
        }
    }
}

struct ServerListView: View {
    @EnvironmentObject var appState: AppState
    @State private var tapCount = 0
    #if DEBUG
    @State private var showPushTest = false
    #endif

    var body: some View {
        List {
            ForEach(appState.pairedServers, id: \.serverId) { server in
                ServerRow(server: server)
            }
        }
        .navigationTitle("Sigil Auth")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { }) {
                    Image(systemName: "plus")
                }
            }
            #if DEBUG
            ToolbarItem(placement: .navigationBarLeading) {
                Menu {
                    Button(action: { showPushTest = true }) {
                        Label("Test Push", systemImage: "paperplane")
                    }
                    Button(action: {
                        Crashlytics.crashlytics().log("Test crash triggered by debug menu")
                        fatalError("Debug crash test")
                    }) {
                        Label("Test Crash", systemImage: "xmark.octagon")
                    }
                } label: {
                    Image(systemName: "hammer.fill")
                        .foregroundColor(.orange)
                }
            }
            #endif
        }
        #if DEBUG
        .sheet(isPresented: $showPushTest) {
            if #available(iOS 16.0, *) {
                PushTestView()
            }
        }
        #endif
    }
}

struct ServerRow: View {
    let server: ServerConfig

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(server.serverName)
                .font(.headline)
            Text(server.serverURL.absoluteString)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }
}
