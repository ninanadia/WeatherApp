package com.idn.ninanadia.weatherapp.models

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @field:SerializedName("coord")
    val coord: Coord,

    @field:SerializedName("weather")
    val weather: List<Weather>,

    @field:SerializedName("base")
    val base: String,

    @field:SerializedName("main")
    val main: Main,

    @field:SerializedName("visibility")
    val visibility: Int,

    @field:SerializedName("wind")
    val wind: Wind,

    @field:SerializedName("rain")
    val rain: Rain,

    @field:SerializedName("clouds")
    val clouds: Clouds,

    @field:SerializedName("dt")
    val dt: Int,

    @field:SerializedName("sys")
    val sys: Sys,

    @field:SerializedName("timezone")
    val timezone: Int,

    @field:SerializedName("Int")
    val id: Int,

    @field:SerializedName("name")
    val name: String,

    @field:SerializedName("cod")
    val cod: Int
)
