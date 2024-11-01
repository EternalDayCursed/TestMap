package com.example.testmaps.presentation.ui

import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.runtime.Error
import com.yandex.mapkit.map.Map as YandexMap

class RouteManager(
    private val map: YandexMap,
    private val mapObjects: MapObjectCollection,
    private val drivingRouter: DrivingRouter,
    private val destinationMarker: PlacemarkMapObject,
    private val markers: List<PlacemarkMapObject>,
    private val updateCancelButtonVisibility: (Boolean) -> Unit
) : DrivingSession.DrivingRouteListener {

    private var drivingSession: DrivingSession? = null
    private var currentRoute: PolylineMapObject? = null
    private var isRouteActive = false

    private val locationManager = MapKitFactory.getInstance().createLocationManager()
    private var locationListener: LocationListener? = null
    private var userPosition: Point? = null

    init {
        hideOtherMarkers()

        map.isScrollGesturesEnabled = false

        locationListener = object : LocationListener {
            override fun onLocationUpdated(location: Location) {
                userPosition = location.position
                updateUserPosition(location.position)
            }

            override fun onLocationStatusUpdated(p0: LocationStatus) {}
        }

        locationManager.subscribeForLocationUpdates(
            0.0,
            1000,
            1.0,
            false,
            FilteringMode.OFF,
            locationListener!!
        )

        isRouteActive = true
    }

    fun buildRoute(userPosition: Point, destination: Point) {
        this.userPosition = userPosition

        val startPoint = RequestPoint(
            userPosition,
            RequestPointType.WAYPOINT,
            null
        )
        val endPoint = RequestPoint(
            destination,
            RequestPointType.WAYPOINT,
            null
        )
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()

        drivingSession?.cancel()
        drivingSession = null

        currentRoute?.let {
            mapObjects.remove(it)
            currentRoute = null
        }

        drivingSession = drivingRouter.requestRoutes(
            listOf(startPoint, endPoint),
            drivingOptions,
            vehicleOptions,
            this
        )

        map.move(
            CameraPosition(userPosition, 18.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1f),
            null
        )
    }

    fun updateUserPosition(userPosition: Point) {
        if (isRouteActive) {
            buildRoute(userPosition, destinationMarker.geometry)
        }
    }

    fun cancelRoute() {
        drivingSession?.cancel()
        drivingSession = null

        currentRoute?.let {
            mapObjects.remove(it)
            currentRoute = null
        }

        showAllMarkers()

        isRouteActive = false

        map.isScrollGesturesEnabled = true

        locationListener?.let {
            locationManager.unsubscribe(it)
        }
        locationListener = null

        updateCancelButtonVisibility(false)
    }

    private fun hideOtherMarkers() {
        for (marker in markers) {
            if (marker != destinationMarker) {
                marker.isVisible = false
            }
        }
    }

    private fun showAllMarkers() {
        for (marker in markers) {
            marker.isVisible = true
        }
    }

    override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
        if (routes.isNotEmpty()) {
            val route = routes.first()

            currentRoute?.let {
                mapObjects.remove(it)
            }

            currentRoute = mapObjects.addPolyline(route.geometry)
        }
    }

    override fun onDrivingRoutesError(error: Error) {
    }
}
