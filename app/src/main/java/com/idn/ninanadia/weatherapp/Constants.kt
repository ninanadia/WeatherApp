package com.idn.ninanadia.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val API_KEY: String = "c6e6de104031f4ec5640336f04e259cf"
    const val METRIC_UNIT: String = "metric"

    //fungsi ini akan membantu kita untuk melihat apakah kita memiliki internet atau tidak
    //atau apakah kita memiliki jaringan atau tidak
    //context adalah sebuah abstract class yang membantu activity saat ini untuk
    //memberikan layanan atau sumberdaya, contoh disini memberikan (sebagai penguhubung) untuk dapat mengakases
    //layanan konektivitas
    fun isNetworkAvailable(context: Context): Boolean {
        //kita membuat object cnnectivityManager yang akan memberi kita layanan konektivitas sistem
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as
                ConnectivityManager

        //kemudian mengecek apakah SDK yang digunakan saat ini lebih besar dari SDK M = 23 atau sama
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //fungsi ini dijalankan ketika SDK nya lebih besar dari 23 atau terbaru
            //disini kita mendeklarasikan akses ke pengelola jaringan, jika jaringan tidak aktif
            // maka fungsi isNetworkAvailable akan mengembalikan nilai false
            val network = connectivityManager.activeNetwork ?: return false
            //disini kita inisialisasi variabel jaringan yang aktif, kemudian kita akan memeriksa kemampuan jaringan
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }

        } else {
            //disini kita menggunakan cara yang sudah lama untuk SDK yang dibawah 23
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}