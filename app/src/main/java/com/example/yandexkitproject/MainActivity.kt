package com.example.yandexkitproject

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.yandexkitproject.databinding.ActivityMainBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.*
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.runtime.Error
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.location.LocationManager
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType

class MainActivity : AppCompatActivity(), InputListener {
    private lateinit var binding: ActivityMainBinding
    private var firstPoint: Point? = null
    private var secondPoint: Point? = null
    private var zoom: Float = 11f
    private lateinit var locationManager: LocationManager
    private var drivingSession: DrivingSession? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            requestUserLocation()
        } else {
            Toast.makeText(this, getString(R.string.PermisionDenied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey(BuildConfig.YANDEX_API_KEY)
        MapKitFactory.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mapVMain.mapWindow.map.move(
            CameraPosition(Point(55.751574, 37.573856), zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )
        binding.mapVMain.mapWindow.map.addInputListener(this)

        locationManager = MapKitFactory.getInstance().createLocationManager()

        binding.btnKemerovo.setOnClickListener {
            zoom = 15f
            binding.mapVMain.mapWindow.map.move(
                CameraPosition(Point(55.354993, 86.085805), zoom, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null
            )
        }

        binding.btnLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                requestUserLocation()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        binding.btnDeleteRoute.setOnClickListener {
            resetRoute()
        }
    }

    private fun requestUserLocation() {
        locationManager.requestSingleUpdate(object : LocationListener {
            override fun onLocationUpdated(location: Location) {
                val userPoint = location.position
                zoom = 15f
                binding.mapVMain.mapWindow.map.move(
                    CameraPosition(userPoint, zoom, 0f, 0f),
                    Animation(Animation.Type.SMOOTH, 0.5f),
                    null
                )
                Toast.makeText(this@MainActivity,
                    getString(R.string.CameraCenteredOnUser), Toast.LENGTH_SHORT).show()
            }

            override fun onLocationStatusUpdated(status: LocationStatus) {
                when (status) {
                    LocationStatus.NOT_AVAILABLE -> {
                        Log.e("MapKit", "Лееее, где мы")
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.CantGetLocation),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    LocationStatus.AVAILABLE -> {
                        Log.d("MapKit", "Всё работает. Нраааааица")
                    }

                    LocationStatus.RESET -> {
                        Log.w("MapKit", "Location service был сброшен")
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.LocationSystemReset),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    override fun onMapTap(map: Map, point: Point) {
        // No action on single tap
    }

    override fun onMapLongTap(map: Map, point: Point) {
        if (firstPoint == null) {
            firstPoint = point
            addPlacemark(point, Color.GREEN, "A")
        } else if (secondPoint == null) {
            secondPoint = point
            addPlacemark(point, Color.RED, "B")
            buildRoute()
        }
    }

    private fun addPlacemark(point: Point, color: Int, text: String) {
        binding.mapVMain.mapWindow.map.mapObjects.addPlacemark(point).apply {
            setText(text)
            setIconStyle(
                IconStyle().apply {
                    setScale(1f)
                }
            )
            setUserData(color)
        }
    }

    private fun buildRoute() {
        val start = firstPoint ?: return
        val end = secondPoint ?: return

        val points = buildList {
            add(RequestPoint(start, RequestPointType.WAYPOINT, null, null, null))
            add(RequestPoint(end, RequestPointType.WAYPOINT, null, null, null))
        }

        val drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(
            DrivingRouterType.ONLINE
        )

        val drivingOptions = DrivingOptions().apply {
            routesCount = 1
        }

        val vehicleOptions = VehicleOptions()

        drivingSession = drivingRouter.requestRoutes(
            points,
            drivingOptions,
            vehicleOptions,
            object : DrivingSession.DrivingRouteListener {
                override fun onDrivingRoutes(routes: List<DrivingRoute>) {
                    if (routes.isNotEmpty()) {
                        showRoute(routes[0])
                    }
                }

                override fun onDrivingRoutesError(error: Error) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.MakingRouteError, error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun showRoute(route: DrivingRoute) {
        binding.mapVMain.mapWindow.map.mapObjects.clear()
        firstPoint?.let { addPlacemark(it, Color.GREEN, "A") }
        secondPoint?.let { addPlacemark(it, Color.RED, "B") }
        binding.mapVMain.mapWindow.map.mapObjects.addPolyline(route.geometry).apply {
            strokeWidth = 5f
            setStrokeColor(Color.BLUE)
        }
    }

    private fun resetRoute() {
        binding.mapVMain.mapWindow.map.mapObjects.clear()
        firstPoint = null
        secondPoint = null
        drivingSession?.cancel()
        drivingSession = null
        Toast.makeText(this, getString(R.string.RouteReset), Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapVMain.onStart()
    }

    override fun onStop() {
        binding.mapVMain.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapVMain.onStop()
        drivingSession?.cancel()
    }
}