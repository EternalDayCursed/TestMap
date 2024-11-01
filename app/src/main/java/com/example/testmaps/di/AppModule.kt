package com.example.testmaps.di

import androidx.room.Room
import com.example.testmaps.data.local.AppDatabase
import com.example.testmaps.data.repository.MarkerRepository
import com.example.testmaps.domain.usecases.AddMarkerUseCase
import com.example.testmaps.domain.usecases.DeleteMarkerUseCase
import com.example.testmaps.domain.usecases.GetMarkersUseCase
import com.example.testmaps.presentation.viewmodels.MapViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel {
        MapViewModel(get(), get(), get())
    }

    factory { GetMarkersUseCase(get()) }
    factory { AddMarkerUseCase(get()) }
    factory { DeleteMarkerUseCase(get()) }

    single { MarkerRepository(get()) }

    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "app_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<AppDatabase>().markerDao() }
}