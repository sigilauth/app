import XCTest

/// Accessibility tests for Aria Phase C review findings
/// Tests WCAG 2.2 AA compliance for iOS app
final class AccessibilityTests: XCTestCase {

    // MARK: - BLOCKING-iOS-1: Pictogram Emoji Names

    func testPictogramEmojiNamesAnnounced() {
        let app = XCUIApplication()
        app.launch()

        let pictogramView = app.otherElements["Device fingerprint"]
        XCTAssertTrue(pictogramView.exists, "Pictogram view should exist")

        let emojiNames = ["apple", "banana", "grapes", "orange", "lemon"]
        for name in emojiNames {
            XCTAssertTrue(
                pictogramView.label.contains(name),
                "Pictogram should announce emoji name '\(name)' for VoiceOver"
            )
        }
    }

    func testPictogramDetailsExpanderAccessible() {
        let app = XCUIApplication()
        app.launch()

        let expander = app.disclosureTriangles["Emoji descriptions"]
        XCTAssertTrue(expander.exists, "Emoji descriptions expander should exist")
        XCTAssertTrue(expander.isEnabled, "Emoji descriptions expander should be enabled")

        expander.tap()

        let firstEmojiDescription = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH '1.'")).firstMatch
        XCTAssertTrue(
            firstEmojiDescription.exists,
            "First emoji description should be visible after expanding"
        )
    }

    // MARK: - BLOCKING-iOS-2: Time Remaining Announcements

    func testTimeRemainingLiveAnnouncements() {
        let app = XCUIApplication()
        app.launch()

        let timeRemainingText = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'Expires'")).firstMatch
        XCTAssertTrue(timeRemainingText.exists, "Time remaining text should exist")

        let initialLabel = timeRemainingText.label
        XCTAssertFalse(initialLabel.isEmpty, "Time remaining should have initial value")

        Thread.sleep(forTimeInterval: 61)

        let updatedLabel = timeRemainingText.label
        XCTAssertNotEqual(
            initialLabel,
            updatedLabel,
            "Time remaining should update after 60 seconds"
        )
    }

    // MARK: - BLOCKING-iOS-3: Biometric Type Detection

    func testBiometricTypeDetection() {
        let app = XCUIApplication()
        app.launch()

        let approveButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Approve with'")).firstMatch
        XCTAssertTrue(approveButton.exists, "Approve button should exist")

        let buttonLabel = approveButton.label

        XCTAssertTrue(
            buttonLabel.contains("Face ID") || buttonLabel.contains("Touch ID") || buttonLabel.contains("biometrics"),
            "Approve button should show correct biometric type (Face ID, Touch ID, or biometrics)"
        )

        XCTAssertFalse(
            buttonLabel.contains("TODO"),
            "Approve button should not contain placeholder text"
        )
    }

    // MARK: - MAJOR-iOS-1: Error Role

    func testNumericCodeErrorHasStaticTextTrait() {
        let app = XCUIApplication()
        app.launch()

        let errorText = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH 'Error:'")).firstMatch

        XCTAssertTrue(errorText.exists, "Error message should exist when invalid code entered")

        XCTAssertTrue(
            errorText.hasStaticTextTrait,
            "Error message should have isStaticText accessibility trait"
        )
    }

    // MARK: - MAJOR-iOS-2: Touch Targets

    func testDigitFieldTouchTargets() {
        let app = XCUIApplication()
        app.launch()

        let firstDigitField = app.textFields.matching(NSPredicate(format: "label CONTAINS 'Digit 1 of 8'")).firstMatch
        XCTAssertTrue(firstDigitField.exists, "First digit field should exist")

        let frame = firstDigitField.frame
        XCTAssertGreaterThanOrEqual(
            frame.width,
            44,
            "Digit field width should be at least 44pt (WCAG 2.5.8 AA)"
        )
        XCTAssertGreaterThanOrEqual(
            frame.height,
            44,
            "Digit field height should be at least 44pt (WCAG 2.5.8 AA)"
        )
    }

    func testAllDigitFieldsMeetMinimumSize() {
        let app = XCUIApplication()
        app.launch()

        for index in 1...8 {
            let digitField = app.textFields.matching(NSPredicate(format: "label CONTAINS 'Digit \(index) of 8'")).firstMatch
            XCTAssertTrue(digitField.exists, "Digit field \(index) should exist")

            let frame = digitField.frame
            XCTAssertGreaterThanOrEqual(
                frame.width,
                44,
                "Digit field \(index) width should be at least 44pt"
            )
            XCTAssertGreaterThanOrEqual(
                frame.height,
                44,
                "Digit field \(index) height should be at least 44pt"
            )
        }
    }

    // MARK: - General Accessibility Tests

    func testApprovalButtonsMinimumSize() {
        let app = XCUIApplication()
        app.launch()

        let denyButton = app.buttons["Deny"]
        XCTAssertTrue(denyButton.exists, "Deny button should exist")

        let denyFrame = denyButton.frame
        XCTAssertGreaterThanOrEqual(
            denyFrame.height,
            44,
            "Deny button height should be at least 44pt"
        )

        let approveButton = app.buttons.matching(NSPredicate(format: "label CONTAINS 'Approve'")).firstMatch
        XCTAssertTrue(approveButton.exists, "Approve button should exist")

        let approveFrame = approveButton.frame
        XCTAssertGreaterThanOrEqual(
            approveFrame.height,
            44,
            "Approve button height should be at least 44pt"
        )
    }

    func testVoiceOverLabelsPresent() {
        let app = XCUIApplication()
        app.launch()

        let elementsWithAccessibility = app.descendants(matching: .any).allElementsBoundByIndex.filter {
            !$0.label.isEmpty
        }

        XCTAssertGreaterThan(
            elementsWithAccessibility.count,
            5,
            "At least 5 elements should have accessibility labels"
        )
    }
}

// MARK: - XCUIElement Extensions

private extension XCUIElement {
    var hasStaticTextTrait: Bool {
        return (self.value(forKey: "accessibilityTraits") as? UInt) == UIAccessibilityTraits.staticText.rawValue
    }
}
