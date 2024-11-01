package com.example.testmaps.domain.usecases

import com.example.testmaps.data.repository.MarkerRepository
import com.example.testmaps.domain.model.Marker
import kotlinx.coroutines.flow.Flow

class GetMarkersUseCase(private val repository: MarkerRepository) {
    operator fun invoke(): Flow<List<Marker>> = repository.getMarkers()
}
