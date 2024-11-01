package com.example.testmaps.domain.usecases

import com.example.testmaps.data.repository.MarkerRepository
import com.example.testmaps.domain.model.Marker

class AddMarkerUseCase(private val repository: MarkerRepository) {
    suspend operator fun invoke(marker: Marker) = repository.addMarker(marker)
}
