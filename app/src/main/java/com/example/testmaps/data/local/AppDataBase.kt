package com.example.testmaps.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.testmaps.domain.model.Marker

@Database(entities = [Marker::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun markerDao(): MarkerDao
}
