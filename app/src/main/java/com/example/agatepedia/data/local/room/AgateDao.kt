package com.example.agatepedia.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.agatepedia.data.local.entity.AgateEntity

@Dao
interface AgateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAgate(agate: AgateEntity)

    @Query("SELECT * FROM agate")
    suspend fun getListAgateBookmark(): List<AgateEntity>

    @Query("DELETE FROM agate WHERE type = :type")
    suspend fun deleteBookmark(type: String)

    @Query("SELECT EXISTS(SELECT * FROM agate where type = :type)")
    suspend fun getAgateBookmark(type: String): Boolean
}