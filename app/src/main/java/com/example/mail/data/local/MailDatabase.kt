package com.example.mail.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EmailMessage::class, Draft::class],
    version = 4,
    exportSchema = true
)
abstract class MailDatabase : RoomDatabase() {

    abstract fun emailDao(): EmailDao

    abstract fun draftDao(): DraftDao

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

        /**
         * v3 -> v4: create `drafts` table for local compose drafts.
         * Fresh table, no data to preserve. Column names avoid SQLite
         * reserved keywords (`to`/`cc`/`bcc` would crash the migration).
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS drafts (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "recipients_to TEXT NOT NULL DEFAULT '', " +
                        "recipients_cc TEXT NOT NULL DEFAULT '', " +
                        "recipients_bcc TEXT NOT NULL DEFAULT '', " +
                        "subject TEXT NOT NULL DEFAULT '', " +
                        "body TEXT NOT NULL DEFAULT '', " +
                        "serverDraftId TEXT, " +
                        "updatedAt INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }
    }
}
