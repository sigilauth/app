package com.wagmilabs.sigil.data.repository

import com.wagmilabs.sigil.core.crypto.KeystoreManager
import com.wagmilabs.sigil.core.crypto.PictogramDerivation
import com.wagmilabs.sigil.data.database.dao.ServerConfigDao
import com.wagmilabs.sigil.data.database.entity.ServerConfigEntity
import com.wagmilabs.sigil.network.SigilApiService
import com.wagmilabs.sigil.network.models.ServerInfo
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.UUID

/**
 * Repository for managing registered servers.
 *
 * Per cascade-data-architecture.md §3:
 * - Multi-server configuration storage
 * - Device key association per server
 * - Server public key caching
 *
 * AGPL-3.0 License
 */
class ServerRepository(
    private val serverConfigDao: ServerConfigDao,
    private val sigilApiService: SigilApiService,
    private val keystoreManager: KeystoreManager
) {

    /**
     * Observes all registered servers.
     */
    fun getAllServers(): Flow<List<ServerConfigEntity>> {
        return serverConfigDao.getAllServers()
    }

    /**
     * Gets server by ID.
     */
    suspend fun getServerById(serverId: String): ServerConfigEntity? {
        return serverConfigDao.getServerById(serverId)
    }

    /**
     * Fetches server info from /info endpoint.
     *
     * @param serverUrl Base URL (e.g., "https://sigil.example.com")
     * @return ServerInfo or null if unreachable
     */
    suspend fun fetchServerInfo(serverUrl: String): ServerInfo? {
        return try {
            // TODO: Create Retrofit instance for this URL
            // For now, assume we have a configured client
            val response = sigilApiService.getServerInfo()
            if (response.isSuccessful) {
                response.body()
            } else {
                Timber.e("Failed to fetch server info: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching server info")
            null
        }
    }

    /**
     * Registers device with server.
     *
     * Flow:
     * 1. Generate keypair in StrongBox
     * 2. Derive device fingerprint and pictogram
     * 3. Fetch server info (/info)
     * 4. Store server config + device key alias
     *
     * @param serverUrl Server base URL
     * @return Registration result
     */
    suspend fun registerWithServer(serverUrl: String): RegistrationResult {
        return try {
            // Fetch server info
            val serverInfo = fetchServerInfo(serverUrl)
                ?: return RegistrationResult.Error("Cannot reach server")

            // Generate device keypair
            val keyAlias = "device_key_${UUID.randomUUID()}"
            val (privateKey, publicKey) = keystoreManager.generateDeviceKeypair(keyAlias)

            // Derive fingerprint and pictogram
            val publicKeyEncoded = publicKey.encoded // TODO: Compress to 33 bytes
            val fingerprint = com.sigilauth.app.core.crypto.CryptoUtils.deriveFingerprint(publicKey)
            val pictogram = PictogramDerivation.derive(fingerprint)

            // Create server config entity
            val serverConfig = ServerConfigEntity(
                serverId = serverInfo.serverId,
                serverName = serverInfo.serverName,
                serverUrl = serverUrl,
                serverPublicKey = serverInfo.serverPublicKey,
                serverPictogram = serverInfo.serverPictogram,
                serverPictogramSpeakable = serverInfo.serverPictogramSpeakable,
                deviceKeyAlias = keyAlias,
                deviceFingerprint = com.sigilauth.app.core.crypto.CryptoUtils.bytesToHex(fingerprint),
                devicePictogram = pictogram.emojis,
                devicePictogramSpeakable = pictogram.speakable,
                registeredAt = System.currentTimeMillis(),
                relayUrl = serverInfo.relayUrl
            )

            // Store in database
            serverConfigDao.insertServer(serverConfig)

            Timber.d("Registered with server: ${serverInfo.serverName}")
            RegistrationResult.Success(serverConfig)

        } catch (e: Exception) {
            Timber.e(e, "Registration failed")
            RegistrationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Removes server and deletes associated device key.
     */
    suspend fun removeServer(serverId: String) {
        val server = serverConfigDao.getServerById(serverId)
        if (server != null) {
            // Delete key from keystore
            keystoreManager.deleteKey(server.deviceKeyAlias)

            // Delete from database
            serverConfigDao.deleteServerById(serverId)

            Timber.d("Removed server: $serverId")
        }
    }

    /**
     * Updates last authentication timestamp.
     */
    suspend fun updateLastAuthTime(serverId: String) {
        serverConfigDao.updateLastAuthTime(serverId, System.currentTimeMillis())
    }

    sealed class RegistrationResult {
        data class Success(val serverConfig: ServerConfigEntity) : RegistrationResult()
        data class Error(val message: String) : RegistrationResult()
    }
}
