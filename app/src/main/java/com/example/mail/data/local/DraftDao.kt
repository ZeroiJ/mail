package com.example.mail.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: Draft): Long

    @Update
    suspend fun update(draft: Draft)

    @Delete
    suspend fun delete(draft: Draft)

    @Query("DELETE FROM drafts WHERE id = :draftId")
    suspend fun deleteById(draftId: Long)

    @Query("SELECT * FROM drafts WHERE id = :draftId")
    suspend fun getById(draftId: Long): Draft?

    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<Draft>>
}
