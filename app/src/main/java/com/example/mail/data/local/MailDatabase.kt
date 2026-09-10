package com.example.mail.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EmailMessage::class],
    version = 3,
    exportSchema = true
)
abstract class MailDatabase : RoomDatabase() {

    abstract fun emailDao(): EmailDao

    companion object {

        /**
         * v1 -> v2: add `summary` (on-device TL;DR) and `bundle_type`
         * (AutoBundler category) columns. Both are nullable-free with
         * sensible defaults, so existing rows migrate losslessly.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE email_messages ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE email_messages ADD COLUMN bundle_type TEXT NOT NULL DEFAULT 'personal'")
            }
        }

        /**
         * v2 -> v3: add `snoozedUntil` (epoch millis) so up-swipe snoozes
         * persist locally. Column defaults to 0 = "not snoozed".
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE email_messages ADD COLUMN snoozedUntil INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
