package com.idn.ninanadia.weatherapp.models

import com.google.gson.annotations.SerializedName

data class Coord (
    @field:SerializedName("lon")
    val lon: Double,

    @field:SerializedName("lan")
    val lat: Double,
)
