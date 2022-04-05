package com.idn.ninanadia.weatherapp.network

import com.idn.ninanadia.weatherapp.models.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

//Kemudian kita membuat interface dari URL
// interface merupakan kelas yang berfungsi untuk implementasi method
// class ini disebut juga class DAO
// https://api.openweathermap.org/data/2.5/weather?lat=35&lon=139&appid={API key}
// BASE URL atau URL Dasar https://api.openweathermap.org/data/
// "2.5/weather" Endpoint bagian URL yang digunakan untuk menentukan aksi
// Parameter ditandai dengan awalan "?" dan pemisah antar parameter "&"
interface WeatherService {

    @GET("2.5/weather")
    fun getWeather(
        //parameter required koordinat geografi
        @Query("lat") lat: Double,
        @Query("lon") lon:Double,
        //parameter units merupakan parameter
        // untuk satuan pengukuran, karna menggunakan
        // celcius maka kita buat valuenya METRIC
        // parameter ini bersifat optional
        @Query("units") units:String?,
        // API Key
        @Query("appid") appid: String?
    ) : Call<WeatherResponse>


}