package com.idn.ninanadia.weatherapp.network

import com.idn.ninanadia.weatherapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//pada class ini kita membuat instance dari retrofit
// class ini berfungsi sebagai jembatan penghubung anatar komponen model atau data class
// dengan komponen service/interface/DAO
// OkHttp merupakan library yang digunakan retrofit untuk dapat berajalan
// Logging Interceptor by OkHttp library tambahan yang digunakan untuk menampilkan hasil response
// pada logcat
class WeatherConfig {
    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/"

        fun getWeatherService(): WeatherService {
            val loggingInterceptor = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            } else {
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE)
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
            // Base URL alamat URL dari REST API
            // addConverterFactory merupakan method yang brfungsi agar data yang sudah dirubah
            // dapat diterima dan dijalankan oleh Retrofit
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
            return retrofit.create(WeatherService::class.java)
        }
    }
}