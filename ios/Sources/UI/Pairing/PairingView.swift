import SwiftUI
import SigilAuthCore

/// Pairing flow entry point
/// Supports 4 transports: QR, 8-digit code, deep link, manual entry
/// Per Aria §2.1: All paths converge on server verification screen
public struct PairingView: View {
    public init() {}

    @State private var selectedMethod: PairingMethod = .qr
    @State private var scannedCode: String?

    public var body: some View {
        NavigationStack {
            VStack(spacing: .s6) {
                VStack(alignment: .leading, spacing: .s2) {
                    Text("Pair Device")
                        .font(.title2)
                        .foregroundColor(.sigilText)
                        .accessibilityAddTraits(.isHeader)
                    Text("Scan the QR code shown by the service you want to pair with.")
                        .font(.body)
                        .foregroundColor(.sigilTextMuted)
                }
                .accessibilityElement(children: .combine)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)

                // Transport selection
                Picker("Pairing Method", selection: $selectedMethod) {
                    Text("Scan QR Code").tag(PairingMethod.qr)
                    Text("Enter Code").tag(PairingMethod.code)
                    Text("Manual Entry").tag(PairingMethod.manual)
                }
                .pickerStyle(.segmented)
                .accessibilityLabel("Pairing method selection")
                .padding(.horizontal)

                // Transport-specific view
                switch selectedMethod {
                case .qr:
                    QRScannerView(scannedCode: $scannedCode)
                case .code:
                    PairingCodeEntryView()
                case .manual:
                    ManualEntryView()
                }

                Spacer()

                Text("Pairing grants this device authorization to approve authentication requests for the paired service.")
                    .font(.caption)
                    .foregroundColor(.sigilTextMuted)
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isStaticText)
                    .padding(.horizontal)
            }
            .padding(.vertical)
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

enum PairingMethod {
    case qr
    case code
    case manual
}

/// 8-digit pairing code entry with enhanced UX
struct PairingCodeEntryView: View {
    @StateObject private var coordinator: PairingCoordinator

    init(coordinator: PairingCoordinator = PairingCoordinator()) {
        _coordinator = StateObject(wrappedValue: coordinator)
    }

    var body: some View {
        NumericCodeEntryView { code in
            Task {
                await coordinator.redeemPairingCode(code)
            }
        }
        .alert(
            "Pairing Error",
            isPresented: .constant(coordinator.error != nil),
            presenting: coordinator.error
        ) { error in
            Button("OK") {
                coordinator.error = nil
            }
        } message: { error in
            Text(error.localizedDescription)
        }
    }
}

struct ManualEntryView: View {
    var body: some View {
        Text("Manual Entry")  // TODO: Implement
    }
}

struct HelpButton: View {
    var body: some View {
        Button {
            // TODO: Show help sheet
        } label: {
            Image(systemName: "questionmark.circle")
                .font(.title2)
        }
        .accessibilityLabel("Help")
        .accessibilityHint("Get help with pairing")
    }
}
