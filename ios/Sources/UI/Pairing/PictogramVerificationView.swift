import SwiftUI
import SigilAuthCore

#if canImport(UIKit)
import UIKit
#endif

/// Pictogram verification screen per AC #4
/// All 4 pairing transports converge here
/// Per AC #6: role="img" + accessible label (PictogramView)
/// Per AC #5: VoiceOver state announcements
@available(iOS 16.0, macOS 14.0, *)
struct PictogramVerificationView: View {
    let serverInfo: ServerInfo
    let onConfirm: () -> Void
    let onReject: () -> Void

    @AccessibilityFocusState private var focusOnPictogram: Bool
    @State private var announced = false

    var body: some View {
        VStack(spacing: 24) {
            // Heading per Aria §2.1
            Text("Verify Server Identity")
                .font(.system(size: 28, weight: .semibold))
                .foregroundColor(.sigilText)
                .accessibilityAddTraits(.isHeader)

            // Subtitle
            Text("Compare this pictogram with the one shown by the service.")
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundColor(.sigilTextMuted)
                .accessibilityHint("Compare the pictogram displayed here with the one on your setup screen")

            // Pictogram display per AC #6
            PictogramView(pictogram: Pictogram(
                emojis: serverInfo.serverPictogram,
                speakable: serverInfo.serverPictogramSpeakable
            ))
            .accessibilityFocused($focusOnPictogram)

            Spacer()

            // Action buttons per Aria §2.2: 44×44pt minimum
            HStack(spacing: .s4) {
                Button {
                    announceRejection()
                    onReject()
                } label: {
                    Text("Reject")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(Color.sigilDanger)
                .foregroundColor(.white)
                .cornerRadius(.rMd)
                .accessibilityLabel("Reject pairing")
                .accessibilityHint("Tap if the pictogram does not match")

                Button {
                    announceConfirmation()
                    onConfirm()
                } label: {
                    Text("Confirm")
                        .frame(maxWidth: .infinity, minHeight: 44)
                }
                .background(Color.sigilSuccess)
                .foregroundColor(.white)
                .cornerRadius(.rMd)
                .accessibilityLabel("Confirm pairing")
                .accessibilityHint("Tap if the pictogram matches")
            }
        }
        .padding()
        .navigationTitle("Verify Server")
        .onAppear {
            // VoiceOver announcement per AC #5
            if !announced {
                focusOnPictogram = true
                announceScreenLoad()
                announced = true
            }
        }
    }

    // MARK: - VoiceOver Announcements (AC #5)

    private func announceScreenLoad() {
        // Announce screen purpose
        let announcement = "Server verification required. Compare the pictogram on this screen with the one shown on your computer."
        #if canImport(UIKit)
        UIAccessibility.post(notification: .announcement, argument: announcement)
        #endif
    }

    private func announceConfirmation() {
        #if canImport(UIKit)
        UIAccessibility.post(notification: .announcement, argument: "Pictogram confirmed. Generating device keys.")
        #endif
    }

    private func announceRejection() {
        #if canImport(UIKit)
        UIAccessibility.post(notification: .announcement, argument: "Pairing cancelled. Pictogram rejected.")
        #endif
    }
}
