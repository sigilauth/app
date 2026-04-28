package com.wagmilabs.sigil.core.utilities

object ClaimCodeValidator {
    private val VALID_CHARACTERS = setOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M',
        'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        '2', '3', '4', '5', '6', '7', '8', '9'
    )

    fun isValid(code: String): Boolean {
        if (code.length != 6) {
            return false
        }

        val uppercase = code.uppercase()
        return uppercase.all { it in VALID_CHARACTERS }
    }

    fun normalize(code: String): String {
        return code.uppercase()
    }

    fun formatForDisplay(code: String): String {
        return if (code.length == 6) {
            "${code.substring(0, 3)}-${code.substring(3)}"
        } else {
            code
        }
    }
}
