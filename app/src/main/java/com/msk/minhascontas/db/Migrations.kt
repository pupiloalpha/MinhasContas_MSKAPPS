package com.msk.minhascontas.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    // Keep a no-op migration from version 4 (legacy) to 5 (Room)
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No-op: we intentionally keep the existing 'contas' table structure as-is so Room can read it.
            // If future schema changes are needed, implement the alter table statements here.
        }
    }
}
