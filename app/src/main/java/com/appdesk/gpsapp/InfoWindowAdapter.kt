package com.appdesk.gpsapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.appdesk.gpsapp.databinding.EmployeeCardBinding
import com.bumptech.glide.Glide
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class InfoWindowAdapter(
    context: Context,
    private val markerAndEmployee: HashMap<Marker, Employee>
) :
    GoogleMap.InfoWindowAdapter {
    private val bindingEmployeeCard: EmployeeCardBinding = EmployeeCardBinding.inflate(LayoutInflater.from(context),null,false)
    override fun getInfoContents(marker: Marker): View? {
        return null
    }

    override fun getInfoWindow(marker: Marker): View? {
        for (key in markerAndEmployee.keys) {
            if(key == marker) {
                return employeeCardDetail(markerAndEmployee[key]!!)
            }
        }
        return null
    }
    private fun employeeCardDetail(employee: Employee):View{
        bindingEmployeeCard.apply {
            jobTitle.text = employee.jobTitle
            completeLesson.text = employee.completedLesson.toString()
            ratingBar.rating = employee.rating
            review.text = employee.numberOfReview.toString()
            price.text = employee.price.toString()
            Glide.with(root).load(employee.imageUrl).into(profileImage)
        }
        return bindingEmployeeCard.root
    }

}