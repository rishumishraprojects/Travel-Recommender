package com.example.travel_recommender

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.travel_recommender.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.fabFindLocations.setOnClickListener {
            if (::mMap.isInitialized) {
                fetchTouristLocations(mMap.cameraPosition.target)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
        checkLocationPermissionAndEnableMyLocation()

        // Info window click listener
        mMap.setOnInfoWindowClickListener { marker ->
            val locationData = marker.tag as? TouristLocation
            if (locationData != null) {
                val intent = Intent(this, DetailActivity::class.java).apply {
                    putExtra("PLACE_LAT", locationData.latitude)
                    putExtra("PLACE_LNG", locationData.longitude)
                    putExtra("PLACE_NAME", locationData.name)
                    putExtra("IMAGE_URL", locationData.imageUrl)
                }
                startActivity(intent)
            }
        }
    }

    private fun fetchTouristLocations(latLng: LatLng) {
        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val request = LocationRequest(latLng.latitude, latLng.longitude)
                val response = RetrofitClient.instance.getNearbyTouristLocations(request)

                if (response.isSuccessful) {
                    mMap.clear()
                    val locations = response.body() ?: emptyList()
                    if (locations.isEmpty()) {
                        Toast.makeText(this@MainActivity, "No locations found here.", Toast.LENGTH_SHORT).show()
                    } else {
                        locations.forEach { location ->
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(LatLng(location.latitude, location.longitude))
                                    .title(location.name)
                            )
                            marker?.tag = location
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Network Error. Is the server running?", Toast.LENGTH_LONG).show()
                Log.e("MainActivity", "API call failed", e)
            }
        }
    }

    // --- Location Permission ---
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
            enableMyLocation()
        }
    }

    private fun checkLocationPermissionAndEnableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        } else {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    private fun enableMyLocation() {
        try {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val startLocation = if (location != null) {
                    LatLng(location.latitude, location.longitude)
                } else {
                    LatLng(25.4195, 81.8848) // Default: Sangam, Prayagraj
                }
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 15f))
            }
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Location permission error.", e)
        }
    }
}
