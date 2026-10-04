/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ua.acclorite.book_story.data.local.dto.ReadingSessionEntity

@Dao
interface ReadingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ReadingSessionEntity)

    @Query("SELECT * FROM ReadingSessionEntity ORDER BY dayEpoch DESC, startedAt DESC")
    suspend fun getAll(): List<ReadingSessionEntity>

    @Query("SELECT * FROM ReadingSessionEntity WHERE dayEpoch BETWEEN :fromDay AND :toDay ORDER BY dayEpoch ASC, startedAt ASC")
    suspend fun getBetween(fromDay: Long, toDay: Long): List<ReadingSessionEntity>

    @Query("DELETE FROM ReadingSessionEntity")
    suspend fun deleteAll()
}