package com.example.testmaps.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.testmaps.domain.model.Marker
import kotlinx.coroutines.flow.Flow

@Dao
interface MarkerDao {
    @Query("SELECT * FROM markers")
    fun getAllMarkers(): Flow<List<Marker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: Marker)

    @Delete
    suspend fun deleteMarker(marker: Marker)
}
