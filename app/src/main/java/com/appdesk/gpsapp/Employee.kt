package com.appdesk.gpsapp

import com.google.android.gms.maps.model.LatLng

data class Employee(
    val jobTitle: String,
    val imageUrl: String,
    val completedLesson: Int,
    val price: Int,
    val rating: Float,
    val numberOfReview: Int,
    val latLng: LatLng
)
