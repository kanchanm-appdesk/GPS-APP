package com.appdesk.gpsapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.*
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.appdesk.gpsapp.databinding.ActivityMainBinding
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

    private val googleApiAvailability by lazy {
        GoogleApiAvailability.getInstance()
    }

    private val googleConnectionStatus by lazy {
        googleApiAvailability.isGooglePlayServicesAvailable(this)
    }

    private lateinit var googleMap: GoogleMap
    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager
    private lateinit var geocoder: Geocoder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        //Initialize fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        //Initialize Geocoder
        geocoder = Geocoder(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun getCurrentLocation() {
        //1. google play
        //2. gps or network/wifi status
        //3. run time location permission

        if (isGooglePlayServicesAvailable()) {
            //check for network and wifi status
            if (isGPSEnabled()) {
                fetchLocation()

            } else {
                //Setting open here
                //Turn on GPS
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            Snackbar.make(binding.root, "Google Play Services Not Installed", Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    //check google play services is exist or not
    private fun isGooglePlayServicesAvailable() = googleConnectionStatus == ConnectionResult.SUCCESS

    //check location Status is enable or disable
    private fun isGPSEnabled(): Boolean {
        locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }


    //request permission for precise and approximate accuracy
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_ACCESS_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Granted
                    fetchLocation()
                } else {
                    //Denied
                    Snackbar.make(
                        binding.root,
                        "Please allow location",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {
                throw Exception("Unrecognized request code $requestCode")
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isRotateGesturesEnabled = false
        googleMap.uiSettings.isScrollGesturesEnabled = false
        googleMap.uiSettings.isTiltGesturesEnabled = false
        googleMap.setOnMarkerClickListener(this)
        getCurrentLocation()
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
        googleMap.isMyLocationEnabled = true
        try {
           // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    //null received
                } else {
                    updateLocation(location)
                }
            }
        } catch (e: Exception){
            Log.e("Exception: %s", e.message, e)
        }

    }

    private fun placeMarkerOnMap(currentLatLong: LatLng) {
        val addresses: List<Address> =
            geocoder.getFromLocation(currentLatLong.latitude,currentLatLong.longitude,
                PERMISSION_REQUEST_ACCESS_LOCATION) as List<Address>
        val address = addresses[0]
        val markerOptions = (MarkerOptions().position(currentLatLong))
        markerOptions.title(address.getAddressLine(0))
//        markerOptions.icon(bitmapFromVector(this,R.drawable.ic_flag))
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        markerOptions.icon
        googleMap.addMarker(markerOptions)
    }

    override fun onMarkerClick(p0: Marker): Boolean = false

    override fun onLocationChanged(location: Location) {
        updateLocation(location)
    }

    private fun updateLocation(location: Location){
        currentLocation = location
        val currentLatLong = LatLng(location.latitude, location.longitude)
        placeMarkerOnMap(currentLatLong)
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong,15f))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.map_options, menu)
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

}