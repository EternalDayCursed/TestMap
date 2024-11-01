package com.example.testmaps

import android.app.Application
import android.util.Log
import com.example.testMaps.BuildConfig
import com.example.testmaps.di.appModule
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.search.SearchFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(listOf(appModule))
        }

        val apiKey = BuildConfig.YANDEX_API_KEY
        Log.d("CRINGE", "API Key: $apiKey")

        if (apiKey.isBlank()) {
            throw AssertionError("API key is not set")
        }

        MapKitFactory.setApiKey(apiKey)
        MapKitFactory.initialize(this)
        DirectionsFactory.initialize(this)
        SearchFactory.initialize(this)
    }
}
