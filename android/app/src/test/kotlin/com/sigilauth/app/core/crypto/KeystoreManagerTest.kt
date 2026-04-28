package com.wagmilabs.sigil.core.crypto

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for KeystoreManager.
 *
 * Note: Full keystore testing requires device hardware.
 * These tests verify basic functionality and error handling.
 *
 * AGPL-3.0 License
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class KeystoreManagerTest {

    private lateinit var keystoreManager: KeystoreManager

    @Before
    fun setup() {
        keystoreManager = KeystoreManager()
    }

    @Test
    fun `isStrongBoxAvailable returns boolean`() {
        // In Robolectric/emulator, StrongBox is typically unavailable
        // Test that method executes without crash
        val available = keystoreManager.isStrongBoxAvailable()
        assertNotNull(available)
        // Expect false in test environment
        assertFalse(available)
    }

    @Test
    fun `listKeys returns list`() {
        val keys = keystoreManager.listKeys()
        assertNotNull(keys)
        // List may be empty in test environment
        assertTrue(keys is List)
    }

    @Test
    fun `getPrivateKey returns null for non-existent key`() {
        val key = keystoreManager.getPrivateKey("non_existent_key_alias")
        assertNull(key)
    }

    @Test
    fun `getPublicKey returns null for non-existent key`() {
        val key = keystoreManager.getPublicKey("non_existent_key_alias")
        assertNull(key)
    }

    @Test
    fun `deleteKey does not crash on non-existent key`() {
        // Should not throw exception
        keystoreManager.deleteKey("non_existent_key")
    }
}
