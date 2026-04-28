package com.wagmilabs.sigil.network.models

import com.google.gson.annotations.SerializedName

/**
 * Server information response from /info endpoint.
 *
 * Per OpenAPI schema and D10: uses pictogram_speakable (space-separated).
 *
 * AGPL-3.0 License
 */
data class ServerInfo(
    @SerializedName("server_id")
    val serverId: String,

    @SerializedName("server_name")
    val serverName: String,

    @SerializedName("server_public_key")
    val serverPublicKey: String,  // Base64-encoded compressed P-256 public key

    @SerializedName("server_pictogram")
    val serverPictogram: List<String>,  // List of 5 emoji names

    @SerializedName("server_pictogram_speakable")
    val serverPictogramSpeakable: String,  // Space-separated per D10

    @SerializedName("version")
    val version: String,

    @SerializedName("mode")
    val mode: String,  // "init" | "operational"

    @SerializedName("relay_url")
    val relayUrl: String? = null,

    @SerializedName("features")
    val features: Features
) {
    data class Features(
        @SerializedName("mpa")
        val mpa: Boolean,

        @SerializedName("secure_decrypt")
        val secureDecrypt: Boolean,

        @SerializedName("mnemonic_generation")
        val mnemonicGeneration: Boolean,

        @SerializedName("webhooks")
        val webhooks: Boolean
    )
}
