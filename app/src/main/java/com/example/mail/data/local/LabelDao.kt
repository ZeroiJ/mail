package com.example.mail.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(labels: List<Label>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(label: Label)

    @Query("DELETE FROM labels WHERE id = :labelId")
    suspend fun deleteById(labelId: String)

    @Query("DELETE FROM labels WHERE type = 'user'")
    suspend fun deleteAllUserLabels()

    @Query("SELECT * FROM labels ORDER BY type DESC, name ASC")
    fun getAll(): Flow<List<Label>>

    @Query("SELECT * FROM labels WHERE id = :labelId")
    suspend fun getById(labelId: String): Label?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCrossRef(ref: EmailLabelCrossRef)

    @Query("DELETE FROM email_label_cross_ref WHERE messageId = :messageId AND labelId = :labelId")
    suspend fun removeCrossRef(messageId: String, labelId: String)

    @Query("DELETE FROM email_label_cross_ref WHERE messageId = :messageId")
    suspend fun clearMessageLabels(messageId: String)

    @Query("SELECT labelId FROM email_label_cross_ref WHERE messageId = :messageId")
    suspend fun getLabelIdsForMessage(messageId: String): List<String>

    @Query("SELECT * FROM labels WHERE id IN (SELECT labelId FROM email_label_cross_ref WHERE messageId = :messageId)")
    fun getLabelsForMessage(messageId: String): Flow<List<Label>>
}
