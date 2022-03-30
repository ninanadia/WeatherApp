package com.idn.ninanadia.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationRequest.create
import com.idn.ninanadia.weatherapp.Constants.API_KEY
import com.idn.ninanadia.weatherapp.Constants.METRIC_UNIT
import com.idn.ninanadia.weatherapp.models.WeatherResponse
import com.idn.ninanadia.weatherapp.network.WeatherConfig
import com.idn.ninanadia.weatherapp.network.WeatherService
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    //tipe FusedLocationProviderClient diperlukan untuk mendapatkan lokasi latitude dan longitude
    //yang akan digunakan untuk pemanggilan API cuaca
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //kode ini berfungsi untuk mengecek apakah pengguna sudah mengaktifkan lokasi atau belum
        if (!isLocationEnabled()) {
            //jika tidak maka aplikasi akan mengeluarkan toast untuk meminta mengaktifkan GPS
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on",
                Toast.LENGTH_SHORT
            ).show()

            //jika tidak diaktifkan akan mengarahkan ke pengaturan dimana
            //memungkinkan kita melakukan perubahan untuk lokasi (mengaktifkan)
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            //Kode ini berfungsi untuk menampilkan dialog izin pada activity dimana disini adalah
            //izin untuk mengakses lokasi saat aplikasi dijalankan
            Dexter.withActivity(this)
                //kita menggunakan metode ini karena kita membutuhkan 2 permissions atau disebut
                //Multiple Permissions
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                //Karena kita memiliki multiple permissions maka listener nya juga bersifat multiple
                .withListener(object : MultiplePermissionsListener {
                    //object diatas memiliki fungsi override untuk mengecek permissions apakah permissions
                    //diterima atau ditolak
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        //jika semua permissions diizinkan maka akan mnejalankan fungsi untuk merequest data lokasi secara spesifik
                        if (report!!.areAllPermissionsGranted()) {
                            requestLocationData()
                        }
                        //Namun jika ada permission yang ditolak, maka akan menampilkan toast
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please enable them as it is mandatory for the app to work",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    //pada fungsi ini kita akan menampilkan dialog untuk permissions
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogPermissions()
                    }

                }).onSameThread()
                .check()

            //jika sudah aktif maka akan menampilkan toast seperti berikut
            Toast.makeText(
                this,
                "Your Location provider is already turned ON.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        }
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()!!
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")

            getLocationWeatherDetails(latitude, longitude)
        }
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (Constants.isNetworkAvailable(this)) {
            val client = WeatherConfig.getWeatherService()
                .getWeather(latitude, longitude, METRIC_UNIT, API_KEY)
            client.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null) {
                        Log.i("Response Result", "$responseBody")
                    } else {
                        Log.e(TAG, "onFailure: ${response.message()}")
                    }

                }
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e(TAG, "Error: ${t.message.toString()}")
                }

            })
        } else {
            Toast.makeText(
                this@MainActivity,
                "No Internet Connection Available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //fungsi ini akan tampil jika pengguna telah menoka untuk mengaktifkan GPS,
    //sehingga aplikasi tida memiliki akses, sehingga aplikasi akan membuat sebuah dialog dengan
    //pesan yang spesifik bahwa permissions tersebut wajib dihidupkan
    private fun showRationalDialogPermissions() {
        AlertDialog.Builder(this)
            .setMessage(
                "It Looks like you have turned off permissions" +
                        " required for this feature. It can be enabled under Application Settings"
            )
            //Kemudian positive button akan menampilkan text yang akan mengarahkan ke Settings jika diklik
            .setPositiveButton(
                "Go To Settings"
            ) { _, _ ->
                try {
                    //mengarhakan kita ke settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    //membutuhkan package untuk membuka pengaturan yang khusus untuk aplikasi kita
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
                //jika ada kesalahan dalam fungsi maka aplikasi akan menuliskan kesalahn tersebut
                //dimana dapat dilihat pada stack trace/tumpukan error di logcat
            }
            //Kemudian tombol negative akan mengatakan Cancel dan dialog kan ditutup
            .setNegativeButton("Cancel") { dialog,
                                           _ ->
                dialog.dismiss()
            }.show()
    }

    private fun isLocationEnabled(): Boolean {
        //menyediakan akses ke layanan lokasi sistem
        //objek locationManager dibuat yang nantinya akan menjadi LOCATION_SERVICE
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //kemudian kita kembalikan hasil dari providernya, berarti GPS diaktifkan
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )

    }
}