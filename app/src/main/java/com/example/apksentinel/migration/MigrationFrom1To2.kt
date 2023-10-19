package com.example.apksentinel.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


class MigrationFrom1To2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add your SQL statements to update the schema here.
        // For example, if you need to add a new column:
        // database.execSQL("ALTER TABLE YourEntity ADD COLUMN new_column_name TYPE");
    }
}