package com.wagmilabs.sigil.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Server configuration entity.
 *
 * Stores one registered server's details.
 * Per D10: pictogram_speakable is space-separated.
 *
 * AGPL-3.0 License
 */
@Entity(tableName = "server_configs")
data class ServerConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "server_id")
    val serverId: String,

    @ColumnInfo(name = "server_name")
    val serverName: String,

    @ColumnInfo(name = "server_url")
    val serverUrl: String,

    @ColumnInfo(name = "server_public_key")
    val serverPublicKey: String,  // Base64-encoded compressed P-256 key

    @ColumnInfo(name = "server_pictogram")
    val serverPictogram: List<String>,  // 5 emoji names

    @ColumnInfo(name = "server_pictogram_speakable")
    val serverPictogramSpeakable: String,  // Space-separated per D10

    @ColumnInfo(name = "device_key_alias")
    val deviceKeyAlias: String,  // Android Keystore alias

    @ColumnInfo(name = "device_fingerprint")
    val deviceFingerprint: String,  // Hex-encoded 64-char

    @ColumnInfo(name = "device_pictogram")
    val devicePictogram: List<String>,  // 5 emoji names

    @ColumnInfo(name = "device_pictogram_speakable")
    val devicePictogramSpeakable: String,  // Space-separated per D10

    @ColumnInfo(name = "registered_at")
    val registeredAt: Long,  // Unix timestamp milliseconds

    @ColumnInfo(name = "last_auth_at")
    val lastAuthAt: Long? = null,  // Last successful authentication

    @ColumnInfo(name = "relay_url")
    val relayUrl: String? = null
)
