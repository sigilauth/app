package com.wagmilabs.sigil.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wagmilabs.sigil.data.database.dao.ServerConfigDao
import com.wagmilabs.sigil.data.database.entity.ServerConfigEntity

/**
 * Room database for Sigil Auth local storage.
 *
 * Per cascade-data-architecture.md §4:
 * - Multi-server configuration storage
 * - Device key aliases
 * - Server public keys
 * - Pictograms
 *
 * AGPL-3.0 License
 */
@Database(
    entities = [
        ServerConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SigilDatabase : RoomDatabase() {

    abstract fun serverConfigDao(): ServerConfigDao

    companion object {
        const val DATABASE_NAME = "sigil_auth.db"
    }
}
