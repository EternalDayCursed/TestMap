package com.example.testmaps.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.testMaps.R
import com.example.testMaps.databinding.FragmentMapBinding
import com.example.testmaps.domain.model.Marker
import com.example.testmaps.presentation.viewmodels.MapViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.FilteringMode
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.yandex.mapkit.map.Map as YandexMap

class MapFragment : Fragment() {

    private val viewModel: MapViewModel by viewModel()
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val mapView get() = binding.mapview
    private lateinit var drivingRouter: DrivingRouter

    private var userLocationLayer: UserLocationLayer? = null
    private var userLocation: Point? = null

    private val mapObjects by lazy { mapView.map.mapObjects.addCollection() }
    private val markers = mutableListOf<PlacemarkMapObject>()

    private var locationListener: LocationListener? = null
    private var inputListener: InputListener? = null

    private lateinit var searchManager: SearchManager
    private var searchSession: Session? = null

    private var routeManager: RouteManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initializeMap()
            binding.btnCancelRoute.setOnClickListener {
                routeManager?.cancelRoute()
                routeManager = null
                updateCancelRouteButtonVisibility(false)
            }

        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ошибка")
                .setMessage("Нет доступа к геолокации")
                .setPositiveButton("Ок", null)
                .show()
        }
    }

    private fun initializeMap() {
        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer?.isVisible = true
        userLocationLayer?.isHeadingEnabled = true

        val locationManager = mapKit.createLocationManager()
        locationListener = object : LocationListener {
            override fun onLocationUpdated(location: Location) {
                userLocation = location.position

                activity?.runOnUiThread {
                    if (routeManager != null) {
                        routeManager?.updateUserPosition(location.position)
                    } else {
                        mapView.map.move(
                            CameraPosition(location.position, 17.0f, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 1f),
                            null
                        )
                    }
                }
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

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.markers.collectLatest { markersList ->
                updateMarkersOnMap(markersList)
            }
        }

        inputListener = object : InputListener {
            override fun onMapTap(map: YandexMap, point: Point) {
                // Обработка нажатия на карту
            }

            override fun onMapLongTap(map: YandexMap, point: Point) {
                showAddMarkerDialog(point)
            }
        }
        mapView.map.addInputListener(inputListener!!)

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE)
    }

    private fun updateMarkersOnMap(markersList: List<Marker>) {
        mapObjects.clear()
        markers.clear()

        markersList.forEach { marker ->
            val placemark = mapObjects.addPlacemark(
                Point(marker.latitude, marker.longitude),
                ImageProvider.fromBitmap(createBitmapFromVector(R.drawable.ic_custom_marker)),
            )

            markers.add(placemark)

            placemark.addTapListener { _, _ ->
                onMarkerClicked(marker)
                true
            }
        }
    }

    private fun showAddMarkerDialog(point: Point) {
        val dialog = AddMarkerDialog { markerName ->
            val marker = Marker(
                name = markerName,
                latitude = point.latitude,
                longitude = point.longitude
            )
            viewModel.addMarker(marker)
        }
        dialog.show(childFragmentManager, "AddMarkerDialog")
    }

    private fun onMarkerClicked(marker: Marker) {
        val point = Point(marker.latitude, marker.longitude)

        searchSession = searchManager.submit(
            point,
            null,
            SearchOptions(),
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val address = response.collection.children.firstOrNull()?.obj?.name
                    showMarkerDialog(marker, address)
                }

                override fun onSearchError(error: Error) {
                    showMarkerDialog(marker, null)
                }
            }
        )
    }



    private fun showMarkerDialog(marker: Marker, address: String?) {
        val message = if (address != null) {
            "Адрес: $address"
        } else {
            "Координаты: ${marker.latitude}, ${marker.longitude}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(marker.name)
            .setMessage(message)
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteMarker(marker)
            }
            .setNegativeButton("Построить маршрут") { _, _ ->
                buildRouteToMarker(marker)
            }
            .show()
    }

    private fun buildRouteToMarker(marker: Marker) {
        val userPosition = userLocation
        if (userPosition != null) {
            val destinationPlacemark = markers.find {
                it.geometry.latitude == marker.latitude && it.geometry.longitude == marker.longitude
            }

            if (destinationPlacemark != null) {
                routeManager = RouteManager(
                    mapView.map,
                    mapObjects,
                    drivingRouter,
                    destinationPlacemark,
                    markers,
                    ::updateCancelRouteButtonVisibility
                )

                routeManager?.buildRoute(userPosition, Point(marker.latitude, marker.longitude))

                updateCancelRouteButtonVisibility(true)
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Ошибка")
                    .setMessage("Не удалось найти выбранную метку на карте")
                    .setPositiveButton("Ок", null)
                    .show()
            }
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ошибка")
                .setMessage("Не удалось определить ваше местоположение")
                .setPositiveButton("Ок", null)
                .show()
        }
    }

    private fun updateCancelRouteButtonVisibility(isVisible: Boolean) {
        binding.btnCancelRoute.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        mapView.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val mapKit = MapKitFactory.getInstance()
        val locationManager = mapKit.createLocationManager()
        locationListener?.let {
            locationManager.unsubscribe(it)
        }
        locationListener = null

        inputListener?.let {
            mapView.map.removeInputListener(it)
        }
        inputListener = null

        markers.clear()

        userLocationLayer = null
        _binding = null
    }

    private fun createBitmapFromVector(art: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(requireContext(), art) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
