package com.example.travel_recommender // CHANGE THIS

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// This should point to your FastAPI server, which runs on port 8000
private const val BASE_URL = "http://10.11.36.73:8000/" // UPDATE YOUR IP

// --- Data Models for Tourist Locations ---
data class LocationRequest(val latitude: Double, val longitude: Double, val radius: Int = 5000)
data class TouristLocation(
    val name: String,
    val place_id: String, // Added place_id to pass to the details screen
    val latitude: Double,
    val longitude: Double,
    val rating: Float?,
    @SerializedName("image_url") val imageUrl: String?
)

// --- Data Models for Place Details (New) ---
data class PlaceDetailRequest(val place_id: String, val place_name: String)
data class PlaceDetailResponse(val history: String)

interface ApiService {
    @POST("tourist-locations/")
    suspend fun getNearbyTouristLocations(@Body request: LocationRequest): Response<List<TouristLocation>>

    // --- New function for getting details ---
    @POST("place-details/")
    suspend fun getPlaceDetails(@Body request: PlaceDetailRequest): Response<PlaceDetailResponse>
}

object RetrofitClient {
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}