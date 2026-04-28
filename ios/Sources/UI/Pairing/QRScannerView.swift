#if canImport(UIKit)
import SwiftUI

/// SwiftUI wrapper for QRScannerViewController
@available(iOS 16.0, *)
struct QRScannerView: UIViewControllerRepresentable {
    @Binding var scannedCode: String?

    func makeUIViewController(context: Context) -> QRScannerViewController {
        let scanner = QRScannerViewController()
        scanner.onCodeScanned = { code in
            scannedCode = code
        }
        return scanner
    }

    func updateUIViewController(_ uiViewController: QRScannerViewController, context: Context) {
        // Update scanner if needed
    }
}
#endif
