package com.wagmilabs.sigil.data.database.dao

import androidx.room.*
import com.wagmilabs.sigil.data.database.entity.ServerConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for server configurations.
 *
 * AGPL-3.0 License
 */
@Dao
interface ServerConfigDao {

    @Query("SELECT * FROM server_configs ORDER BY registered_at DESC")
    fun getAllServers(): Flow<List<ServerConfigEntity>>

    @Query("SELECT * FROM server_configs WHERE server_id = :serverId")
    suspend fun getServerById(serverId: String): ServerConfigEntity?

    @Query("SELECT * FROM server_configs WHERE device_fingerprint = :fingerprint LIMIT 1")
    suspend fun getServerByDeviceFingerprint(fingerprint: String): ServerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerConfigEntity)

    @Update
    suspend fun updateServer(server: ServerConfigEntity)

    @Delete
    suspend fun deleteServer(server: ServerConfigEntity)

    @Query("DELETE FROM server_configs WHERE server_id = :serverId")
    suspend fun deleteServerById(serverId: String)

    @Query("SELECT COUNT(*) FROM server_configs")
    suspend fun getServerCount(): Int

    @Query("UPDATE server_configs SET last_auth_at = :timestamp WHERE server_id = :serverId")
    suspend fun updateLastAuthTime(serverId: String, timestamp: Long)
}
