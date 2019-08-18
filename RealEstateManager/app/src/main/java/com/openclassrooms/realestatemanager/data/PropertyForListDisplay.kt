package com.openclassrooms.realestatemanager.data

/**
 * Created by galou on 2019-08-12
 */
data class PropertyForListDisplay(
        val id: Int, val type: String,
        val neighborhood: String, val lat: Double,
        val lng: Double, val price: Double,
        val pictureUrl: String?, val sold: Boolean)