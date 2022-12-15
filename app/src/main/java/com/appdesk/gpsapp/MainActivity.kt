package com.appdesk.gpsapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.appdesk.gpsapp.databinding.ActivityMainBinding
import com.appdesk.gpsapp.databinding.MapPinBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar


private const val PERMISSION_REQUEST_ACCESS_LOCATION = 1

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, LocationListener {

    //for receiving location updates
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding
    private lateinit var googleMap: GoogleMap
    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private lateinit var geocoder: Geocoder
    private lateinit var mapPin: MapPinBinding

    private val googleApiAvailability by lazy {
        GoogleApiAvailability.getInstance()
    }

    private val googleConnectionStatus by lazy {
        googleApiAvailability.isGooglePlayServicesAvailable(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapPin = MapPinBinding.inflate(layoutInflater)
    }

    override fun onStart() {
        super.onStart()
        //Initialize fusedLocationProviderClient
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this@MainActivity)
        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //Initialize Geocoder
        geocoder = Geocoder(this@MainActivity)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this@MainActivity)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.map_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //Granted
        //Denied
        when (requestCode) {
            PERMISSION_REQUEST_ACCESS_LOCATION -> when {
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED -> fetchLocation()
                else -> Snackbar.make(
                    binding.root,
                    "Please allow location",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            else -> throw Exception("Unrecognized request code $requestCode")
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isRotateGesturesEnabled = false
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = false
        }
        googleMap.setOnMarkerClickListener(this)
        getCurrentLocation()
    }

    override fun onMarkerClick(p0: Marker): Boolean = false

    override fun onLocationChanged(location: Location) = updateLocation(location)

    private fun getCurrentLocation() {
        // check google play services is available or not
        //check for network and wifi status
        when {
            isGooglePlayServicesAvailable() ->
                //Setting open here
                //Turn on GPS
                when {
                    isGPSEnabled() -> fetchLocation()
                    else -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            else -> Snackbar.make(
                binding.root,
                "Google Play Services Not Installed",
                Snackbar.LENGTH_SHORT
            )
                .show()
        }
    }

    //check google play services is exist or not
    private fun isGooglePlayServicesAvailable() = googleConnectionStatus == ConnectionResult.SUCCESS

    //check location Status is enable or disable
    private fun isGPSEnabled(): Boolean =
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )

    private fun fetchLocation() {//null received
        // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            -> {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                requestPermission()
                return
            }
            else -> {
                googleMap.isMyLocationEnabled = true
                try {
                    // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                        location?.let { updateLocation(it) }
                    }
                } catch (e: Exception) {
                    Log.e("Exception: %s", e.message, e)
                }
            }
        }
    }

    private fun updateLocation(location: Location) {
        currentLocation = location
        val currentLatLong = LatLng(location.latitude, location.longitude)
        placeMarkerOnMap(currentLatLong)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 15f))
    }

    private fun placeMarkerOnMap(currentLatLong: LatLng) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                geocoder.getFromLocation(
                    currentLatLong.latitude, currentLatLong.longitude,
                    PERMISSION_REQUEST_ACCESS_LOCATION
                ) {
                    markerOption(it, currentLatLong)
                }
            else -> {
                val addresses: List<Address> =
                    geocoder.getFromLocation(
                        currentLatLong.latitude, currentLatLong.longitude,
                        PERMISSION_REQUEST_ACCESS_LOCATION
                    ) as List<Address>
                markerOption(addresses, currentLatLong)
            }
        }
    }

    //request permission for precise and approximate accuracy
    private fun requestPermission() = ActivityCompat.requestPermissions(
        this@MainActivity,
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        PERMISSION_REQUEST_ACCESS_LOCATION
    )

    // Convert Vector file into bitmap
    private fun bitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        // below line is use to generate a drawable.
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)

        // below line is use to set bounds to our vector drawable.
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )

        // below line is use to create a bitmap for our
        // drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Convert View into bitmap
    private fun bitmapFromView(view: View): Bitmap {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val bitmap =
            Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.draw(canvas)
        return bitmap
    }

    private fun markerOption(addresses: List<Address>, currentLatLong: LatLng) {
        val employee1 = Employee(
            "QA",
            "https://cdn.mos.cms.futurecdn.net/p5quSf4dZXctG9WFepXFdR-320-30.jpg",
            5,
            15,
            4.0f,
            4,
            currentLatLong
        )
        val employee2 = Employee(
            "SDE",
            "https://images.unsplash.com/photo-1613064756072-52b429a1e06f?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxzZWFyY2h8MTR8fGJsYWNrJTIwYW5kJTIwd2hpdGUlMjBwb3J0cmFpdHxlbnwwfHwwfHw%3D&w=320&q=30",
            7,
            30,
            4.5f,
            13,
            LatLng(28.481000, 77.085801)
        )
        val employee3 = Employee(
            "SSE",
            "https://img.freepik.com/free-photo/portrait-white-man-isolated_53876-40306.jpg?w=320",
            19,
            52,
            4.5f,
            37,
            LatLng(28.452866, 77.074508)
        )
        val employees = listOf(employee1, employee2, employee3)
        val markerAndEmployee = HashMap<Marker,Employee>()
        for (i in employees.indices) {
            val markerOptions = (MarkerOptions().position(employees[i].latLng)).apply {
                mapPin.jobTitle.text = employees[i].jobTitle
                val bitmap1 = Bitmap.createScaledBitmap(bitmapFromView(mapPin.root), mapPin.mapPin.width, mapPin.mapPin.height, false)
                val smallMarkerIcon1 = BitmapDescriptorFactory.fromBitmap(bitmap1)
                icon(smallMarkerIcon1)
            }
            val marker = googleMap.addMarker(markerOptions)
            markerAndEmployee[marker!!] = employees[i]
        }
        googleMap.setInfoWindowAdapter(InfoWindowAdapter(baseContext, markerAndEmployee))
    }
}