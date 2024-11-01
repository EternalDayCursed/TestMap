package com.example.testmaps.data.repository

import com.example.testmaps.data.local.MarkerDao
import com.example.testmaps.domain.model.Marker
import kotlinx.coroutines.flow.Flow

class MarkerRepository(private val markerDao: MarkerDao) {

    fun getMarkers(): Flow<List<Marker>> = markerDao.getAllMarkers()

    suspend fun addMarker(marker: Marker) = markerDao.insertMarker(marker)

    suspend fun deleteMarker(marker: Marker) = markerDao.deleteMarker(marker)
}
