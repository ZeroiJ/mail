package com.example.mail.di

import android.content.Context
import androidx.room.Room
import com.example.mail.data.local.EmailDao
import com.example.mail.data.local.MailDatabase
import com.example.mail.data.repository.EmailRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMailDatabase(@ApplicationContext context: Context): MailDatabase {
        return Room.databaseBuilder(
            context,
            MailDatabase::class.java,
            "mail.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideEmailDao(database: MailDatabase): EmailDao = database.emailDao()

    @Provides
    @Singleton
    fun provideEmailRepository(dao: EmailDao): EmailRepository = EmailRepository(dao)
}
