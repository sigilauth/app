# Error codes per Aria §5 (field + problem + solution)
error-network = Connection failed. Check your network and try again.
error-timeout = Request timed out. Please try again.
error-invalid-qr = Invalid QR code. Scan a Sigil Auth registration code.
error-invalid-code = Invalid pairing code. Check the code and try again. { $attemptsRemaining ->
    [one] { $attemptsRemaining } attempt remaining.
   *[other] { $attemptsRemaining } attempts remaining.
}
error-code-expired = Pairing code expired. Request a new code.
error-code-attempts = Too many attempts. Request a new code.
error-server-unreachable = Cannot reach server. Verify the URL is correct.
error-attestation-failed = Device attestation failed. This device may not be supported.
error-biometric-failed = Authentication failed. Try again.
error-biometric-cancelled = Authentication cancelled.
error-biometric-not-enrolled = Set up { $biometricType } in your device settings to continue.
error-challenge-expired = Request expired. Ask for a new approval request.
error-already-responded = Already responded to this request.
error-push-permission-denied = Turn on notifications in Settings to receive approval requests.
error-server-not-registered = This server isn't registered on your device.
error-device-removed = This device was removed from { $serverName }. Re-register to continue.
