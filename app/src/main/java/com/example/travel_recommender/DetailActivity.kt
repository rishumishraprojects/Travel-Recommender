package com.example.travel_recommender

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class DetailActivity : AppCompatActivity() {

    private lateinit var historyTextView: TextView
    private lateinit var historyProgressBar: ProgressBar
    private lateinit var tts: TextToSpeech
    private lateinit var readFab: FloatingActionButton
    private lateinit var stopFab: FloatingActionButton
    private lateinit var navigateFab: FloatingActionButton
    private lateinit var placeName: String
    private var placeLat: Double = 0.0
    private var placeLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        historyTextView = findViewById(R.id.history_text)
        historyProgressBar = findViewById(R.id.history_loading)
        val detailImage: ImageView = findViewById(R.id.detail_image)
        readFab = findViewById(R.id.tts_fab)
        stopFab = findViewById(R.id.fab_stop_history)
        navigateFab = findViewById(R.id.navigate_fab)

        stopFab.hide() // initially hidden

        placeName = intent.getStringExtra("PLACE_NAME") ?: "Unknown"
        placeLat = intent.getDoubleExtra("PLACE_LAT", 0.0)
        placeLng = intent.getDoubleExtra("PLACE_LNG", 0.0)
        val imageUrl = intent.getStringExtra("IMAGE_URL")

        // Load image
        Glide.with(this).load(imageUrl).into(detailImage)

        // Initialize TTS
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale("en", "IN") // Indian English
            }
        }

        readFab.setOnClickListener {
            val history = historyTextView.text.toString()
            if (history.isNotBlank()) {
                tts.speak(history, TextToSpeech.QUEUE_FLUSH, null, "HISTORY_ID")
                stopFab.show()
            } else {
                Toast.makeText(this, "History not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }

        stopFab.setOnClickListener {
            if (tts.isSpeaking) {
                tts.stop()
                stopFab.hide()
            }
        }

        navigateFab.setOnClickListener {
            val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$placeLat,$placeLng&travelmode=driving")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }

        // Load history asynchronously
        loadHistory()
    }

    private fun loadHistory() {
        historyProgressBar.visibility = View.VISIBLE
        historyTextView.text = ""
        // simulate API call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Here, call your API to fetch history
                val history = RetrofitClient.instance.getPlaceDetails(
                    PlaceDetailRequest(place_id = "", place_name = placeName)
                ).body()?.history ?: "History not found."

                withContext(Dispatchers.Main) {
                    historyTextView.text = history
                    historyProgressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    historyTextView.text = "Failed to load history."
                    historyProgressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (tts.isSpeaking) tts.stop()
        tts.shutdown()
    }
}
