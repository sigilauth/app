package com.wagmilabs.sigil.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wagmilabs.sigil.data.database.entity.ServerConfigEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for ServerConfigDao (Room database).
 *
 * Uses in-memory database for testing.
 *
 * AGPL-3.0 License
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ServerConfigDaoTest {

    private lateinit var database: SigilDatabase
    private lateinit var serverConfigDao: com.sigilauth.app.data.database.dao.ServerConfigDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SigilDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        serverConfigDao = database.serverConfigDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insertServer and getServerById work correctly`() = runTest {
        val server = createTestServer("server1")

        serverConfigDao.insertServer(server)
        val retrieved = serverConfigDao.getServerById("server1")

        assertNotNull(retrieved)
        assertEquals(server.serverId, retrieved?.serverId)
        assertEquals(server.serverName, retrieved?.serverName)
    }

    @Test
    fun `getAllServers returns all inserted servers`() = runTest {
        val server1 = createTestServer("server1")
        val server2 = createTestServer("server2")

        serverConfigDao.insertServer(server1)
        serverConfigDao.insertServer(server2)

        val allServers = serverConfigDao.getAllServers().first()

        assertEquals(2, allServers.size)
        assertTrue(allServers.any { it.serverId == "server1" })
        assertTrue(allServers.any { it.serverId == "server2" })
    }

    @Test
    fun `deleteServerById removes server`() = runTest {
        val server = createTestServer("server1")

        serverConfigDao.insertServer(server)
        assertEquals(1, serverConfigDao.getServerCount())

        serverConfigDao.deleteServerById("server1")
        assertEquals(0, serverConfigDao.getServerCount())

        val retrieved = serverConfigDao.getServerById("server1")
        assertNull(retrieved)
    }

    @Test
    fun `updateLastAuthTime updates timestamp`() = runTest {
        val server = createTestServer("server1")
        serverConfigDao.insertServer(server)

        val newTimestamp = System.currentTimeMillis()
        serverConfigDao.updateLastAuthTime("server1", newTimestamp)

        val updated = serverConfigDao.getServerById("server1")
        assertEquals(newTimestamp, updated?.lastAuthAt)
    }

    @Test
    fun `getServerByDeviceFingerprint returns correct server`() = runTest {
        val server = createTestServer("server1", fingerprint = "abc123")
        serverConfigDao.insertServer(server)

        val retrieved = serverConfigDao.getServerByDeviceFingerprint("abc123")

        assertNotNull(retrieved)
        assertEquals("server1", retrieved?.serverId)
    }

    @Test
    fun `insert with onConflict REPLACE updates existing server`() = runTest {
        val server1 = createTestServer("server1", serverName = "Original Name")
        val server2 = createTestServer("server1", serverName = "Updated Name")

        serverConfigDao.insertServer(server1)
        serverConfigDao.insertServer(server2) // Should replace

        val retrieved = serverConfigDao.getServerById("server1")
        assertEquals("Updated Name", retrieved?.serverName)
        assertEquals(1, serverConfigDao.getServerCount())
    }

    private fun createTestServer(
        serverId: String,
        serverName: String = "Test Server",
        fingerprint: String = "test_fingerprint"
    ): ServerConfigEntity {
        return ServerConfigEntity(
            serverId = serverId,
            serverName = serverName,
            serverUrl = "https://test.example.com",
            serverPublicKey = "test_server_pk",
            serverPictogram = listOf("apple", "banana", "car", "dog", "tree"),
            serverPictogramSpeakable = "apple banana car dog tree",
            deviceKeyAlias = "test_device_key",
            deviceFingerprint = fingerprint,
            devicePictogram = listOf("fire", "moon", "star", "key", "bell"),
            devicePictogramSpeakable = "fire moon star key bell",
            registeredAt = System.currentTimeMillis()
        )
    }
}
