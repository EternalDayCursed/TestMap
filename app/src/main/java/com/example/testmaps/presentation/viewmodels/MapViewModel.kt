package com.example.testmaps.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testmaps.domain.model.Marker
import com.example.testmaps.domain.usecases.AddMarkerUseCase
import com.example.testmaps.domain.usecases.DeleteMarkerUseCase
import com.example.testmaps.domain.usecases.GetMarkersUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(
    getMarkersUseCase: GetMarkersUseCase,
    private val addMarkerUseCase: AddMarkerUseCase,
    private val deleteMarkerUseCase: DeleteMarkerUseCase
) : ViewModel() {

    val markers: StateFlow<List<Marker>> = getMarkersUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun addMarker(marker: Marker) {
        viewModelScope.launch {
            addMarkerUseCase(marker)
        }
    }

    fun deleteMarker(marker: Marker) {
        viewModelScope.launch {
            deleteMarkerUseCase(marker)
        }
    }
}
