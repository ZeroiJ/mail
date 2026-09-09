package com.example.mail.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EmailMessage::class],
    version = 1,
    exportSchema = true
)
abstract class MailDatabase : RoomDatabase() {

    abstract fun emailDao(): EmailDao

    companion object {
        @Volatile
        private var INSTANCE: MailDatabase? = null

        fun getInstance(context: Context): MailDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MailDatabase::class.java,
                    "mail.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
