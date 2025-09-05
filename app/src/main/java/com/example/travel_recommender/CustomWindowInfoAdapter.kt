package com.example.travel_recommender

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    private val logTag = "InfoWindowDebug"

    override fun getInfoWindow(marker: Marker): View? = null

    override fun getInfoContents(marker: Marker): View {
        Log.d(logTag, "1. getInfoContents called for marker: ${marker.title}")

        val view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)

        val titleTextView = view.findViewById<TextView>(R.id.info_title)
        val ratingBar = view.findViewById<RatingBar>(R.id.info_rating)
        val imageView = view.findViewById<ImageView>(R.id.info_image)

        val locationData = marker.tag as? TouristLocation ?: return view

        titleTextView.text = locationData.name
        locationData.rating?.let {
            ratingBar.visibility = View.VISIBLE
            ratingBar.rating = it
        } ?: run {
            ratingBar.visibility = View.GONE
        }

        // Load image with Glide using CustomTarget
        if (!locationData.imageUrl.isNullOrEmpty()) {
            Log.d(logTag, "2. Image URL found: ${locationData.imageUrl}")

            Glide.with(context)
                .load(locationData.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        imageView.setImageDrawable(resource)
                        if (marker.isInfoWindowShown) {
                            Log.d(logTag, "3. Refreshing info window after image load")
                            marker.hideInfoWindow()
                            marker.showInfoWindow()
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        imageView.setImageDrawable(placeholder)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Log.e(logTag, "Glide failed to load image")
                        imageView.setImageDrawable(errorDrawable ?: context.getDrawable(android.R.drawable.ic_menu_gallery))
                    }
                })
        } else {
            Log.d(logTag, "No image URL found for this location.")
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        return view
    }
}
