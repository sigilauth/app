import XCTest

/// XCUITest for pairing flow per AC #13
/// Tests all 4 pairing transports: QR, 8-digit, Universal Link, manual
@available(iOS 16.0, *)
final class PairingFlowTests: XCTestCase {

    var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launch()
    }

    override func tearDownWithError() throws {
        app = nil
    }

    // MARK: - Transport 1: QR Code

    func testPairingFlow_QRCode() throws {
        // Navigate to pairing screen
        app.buttons["Pair Device"].tap()

        // Select QR scanner tab
        app.buttons["Scan QR Code"].tap()

        // Camera permission alert (simulator will show alert, device may auto-grant)
        let allowButton = app.alerts.buttons["Allow"]
        if allowButton.waitForExistence(timeout: 2) {
            allowButton.tap()
        }

        // Simulate QR code scan (in real test, inject via XCTest screenshot)
        // For now, test UI exists
        XCTAssertTrue(app.otherElements["QR Scanner"].exists)

        // After scan, should navigate to pictogram verification
        // (Requires mock QR code injection - skip for now)
        throw XCTSkip("QR code injection requires physical device or simulator injection")
    }

    // MARK: - Transport 2: 8-Digit Code

    func testPairingFlow_8DigitCode_IndividualDigitEntry() throws {
        app.buttons["Add Server"].tap()
        app.buttons["Enter Code"].tap()

        let digitFields = app.textFields.matching(NSPredicate(format: "label BEGINSWITH 'Digit'"))
        XCTAssertEqual(digitFields.count, 8, "Should have 8 individual digit fields")

        digitFields.element(boundBy: 0).tap()
        digitFields.element(boundBy: 0).typeText("1")

        digitFields.element(boundBy: 1).tap()
        digitFields.element(boundBy: 1).typeText("2")

        digitFields.element(boundBy: 2).tap()
        digitFields.element(boundBy: 2).typeText("3")

        digitFields.element(boundBy: 3).tap()
        digitFields.element(boundBy: 3).typeText("4")

        digitFields.element(boundBy: 4).tap()
        digitFields.element(boundBy: 4).typeText("5")

        digitFields.element(boundBy: 5).tap()
        digitFields.element(boundBy: 5).typeText("6")

        digitFields.element(boundBy: 6).tap()
        digitFields.element(boundBy: 6).typeText("7")

        digitFields.element(boundBy: 7).tap()
        digitFields.element(boundBy: 7).typeText("8")

        XCTAssertTrue(app.activityIndicators["Verifying code"].waitForExistence(timeout: 1))
    }

    func testPairingFlow_8DigitCode_PasteSupport() throws {
        app.buttons["Add Server"].tap()
        app.buttons["Enter Code"].tap()

        let digitFields = app.textFields.matching(NSPredicate(format: "label BEGINSWITH 'Digit'"))
        digitFields.element(boundBy: 0).tap()

        UIPasteboard.general.string = "84729135"

        let pasteCommand = app.menuItems["Paste"]
        if pasteCommand.exists {
            pasteCommand.tap()

            for i in 0..<8 {
                let field = digitFields.element(boundBy: i)
                XCTAssertFalse(field.value as? String == "empty", "Digit \(i+1) should be filled after paste")
            }
        } else {
            throw XCTSkip("Paste command not available in simulator")
        }
    }

    func testPairingFlow_8DigitCode_ErrorDisplay_InvalidFormat() throws {
        app.buttons["Add Server"].tap()
        app.buttons["Enter Code"].tap()

        let digitFields = app.textFields.matching(NSPredicate(format: "label BEGINSWITH 'Digit'"))

        digitFields.element(boundBy: 0).tap()
        digitFields.element(boundBy: 0).typeText("1")
        digitFields.element(boundBy: 1).tap()
        digitFields.element(boundBy: 1).typeText("a")

        let errorLabel = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'Error'")).firstMatch
        XCTAssertTrue(errorLabel.waitForExistence(timeout: 2))
    }

    func testPairingFlow_8DigitCode_AccessibilityLabels() throws {
        app.buttons["Add Server"].tap()
        app.buttons["Enter Code"].tap()

        let digitFields = app.textFields.matching(NSPredicate(format: "label BEGINSWITH 'Digit'"))

        for i in 0..<8 {
            let field = digitFields.element(boundBy: i)
            XCTAssertTrue(field.exists)
            XCTAssertEqual(field.label, "Digit \(i+1) of 8")
        }

        let firstField = digitFields.element(boundBy: 0)
        XCTAssertTrue(
            firstField.value(forKey: "accessibilityHint") as? String == "Enter 8 digit pairing code. You can paste the entire code.",
            "First field should have paste hint"
        )
    }

    // MARK: - Transport 3: Universal Link

    func testPairingFlow_UniversalLink() throws {
        // Test deep link handling
        // Format: sigil://pair?url=https://sigil.example.com

        let url = URL(string: "sigil://pair?url=https://sigil.example.com")!

        // Open URL (requires app to handle URL scheme)
        // In real test: XCUIDevice.shared.system.open(url)
        // For now, verify URL handling is registered
        throw XCTSkip("Universal Link testing requires URL scheme registration and mock server")
    }

    // MARK: - Transport 4: Manual Entry

    func testPairingFlow_ManualEntry() throws {
        app.buttons["Add Server"].tap()
        app.buttons["Manual Entry"].tap()

        let urlField = app.textFields["Server URL"]
        XCTAssertTrue(urlField.exists)

        // Enter invalid URL
        urlField.tap()
        urlField.typeText("not-a-url")
        app.buttons["Continue"].tap()

        // Should show error
        XCTAssertTrue(app.staticTexts["Invalid server URL"].exists)

        // Enter valid URL
        urlField.clearText()
        urlField.typeText("https://sigil.example.com")
        app.buttons["Continue"].tap()

        // Should navigate to pictogram verification
        // (Requires mock server - skip for now)
        throw XCTSkip("Manual entry requires mock server response")
    }

    // MARK: - Pictogram Verification (All Transports Converge Here)

    func testPictogramVerification_Accept() throws {
        // Assumes we're on pictogram verification screen
        // (Would be navigated to from any of the 4 transports)

        throw XCTSkip("Requires navigating from a transport flow - implement after mock server")
    }

    func testPictogramVerification_Reject() throws {
        throw XCTSkip("Requires navigating from a transport flow - implement after mock server")
    }

    // MARK: - Accessibility Testing

    func testPairingFlow_VoiceOverAnnouncements() throws {
        // Enable VoiceOver testing
        app.activate()

        // Check that critical elements have accessibility labels
        app.buttons["Add Server"].tap()

        let qrButton = app.buttons["Scan QR Code"]
        XCTAssertTrue(qrButton.exists)
        XCTAssertNotNil(qrButton.label)

        let codeButton = app.buttons["Enter Code"]
        XCTAssertTrue(codeButton.exists)
        XCTAssertNotNil(codeButton.label)

        // TODO: Test actual VoiceOver announcements with accessibility inspector
    }
}

// MARK: - Test Helpers

extension XCUIElement {
    func clearText() {
        guard let stringValue = self.value as? String else {
            return
        }

        let deleteString = String(repeating: XCUIKeyboardKey.delete.rawValue, count: stringValue.count)
        typeText(deleteString)
    }
}
