package com.wagmilabs.sigil.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wagmilabs.sigil.network.models.TrustedServer
import java.util.Date

interface TrustStorageService {
    fun saveTrustedServer(server: TrustedServer)
    fun loadTrustedServer(fingerprint: String): TrustedServer?
    fun loadAllTrustedServers(): List<TrustedServer>
    fun removeTrustedServer(fingerprint: String)
}

class EncryptedTrustStorage(context: Context) : TrustStorageService {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "trusted_servers",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .create()

    override fun saveTrustedServer(server: TrustedServer) {
        val json = gson.toJson(server)
        sharedPreferences.edit()
            .putString(server.serverFingerprint, json)
            .apply()
    }

    override fun loadTrustedServer(fingerprint: String): TrustedServer? {
        val json = sharedPreferences.getString(fingerprint, null) ?: return null
        return try {
            gson.fromJson(json, TrustedServer::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun loadAllTrustedServers(): List<TrustedServer> {
        return sharedPreferences.all.values.mapNotNull { value ->
            try {
                gson.fromJson(value as? String, TrustedServer::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun removeTrustedServer(fingerprint: String) {
        sharedPreferences.edit()
            .remove(fingerprint)
            .apply()
    }
}
