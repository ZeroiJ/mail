package com.example.mail.di

import android.content.Context
import androidx.room.Room
import com.example.mail.data.local.DraftDao
import com.example.mail.data.local.EmailDao
import com.example.mail.data.local.LabelDao
import com.example.mail.data.local.MailDatabase
import com.example.mail.data.repository.EmailRepositoryImpl
import com.example.mail.domain.repository.EmailRepository
import com.example.mail.util.security.CryptoManager
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
        // SQLCipher SupportFactory wraps the Keystore-backed AES-256 master key,
        // so encryption applies from first creation (per AGENTS.md).
        return Room.databaseBuilder(
            context,
            MailDatabase::class.java,
            "mail.db"
        )
            .openHelperFactory(CryptoManager.getOrCreateSupportFactory(context))
            .addMigrations(
                MailDatabase.MIGRATION_1_2,
                MailDatabase.MIGRATION_2_3,
                MailDatabase.MIGRATION_3_4,
                MailDatabase.MIGRATION_4_5,
                MailDatabase.MIGRATION_5_6,
                MailDatabase.MIGRATION_6_7
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideEmailDao(database: MailDatabase): EmailDao = database.emailDao()

    @Provides
    @Singleton
    fun provideDraftDao(database: MailDatabase): DraftDao = database.draftDao()

    @Provides
    @Singleton
    fun provideLabelDao(database: MailDatabase): LabelDao = database.labelDao()

    @Provides
    @Singleton
    fun provideEmailRepository(impl: EmailRepositoryImpl): EmailRepository = impl
}
