package com.idn.ninanadia.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
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
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
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
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    //tipe FusedLocationProviderClient diperlukan untuk mendapatkan lokasi latitude dan longitude
    //yang akan digunakan untuk pemanggilan API cuaca
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog: Dialog? = null

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

            showCustomProgressDialog()

            //fungsi enqueue digunakan untuk menjalankan request
            //SECARA asynchronous di background, sehingga aplikasi tidak freeze ketika
            //melakukan request
            client.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    val responseBody = response.body()
                    if (response.isSuccessful && responseBody != null) {

                        hideProgressDialog()
                        setupUI(responseBody)
                        Log.i("Response Result", "$responseBody")
                    } else {
                        Log.e(TAG, "onFailure: ${response.message()}")
                    }

                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e(TAG, "Error: ${t.message.toString()}")
                    hideProgressDialog()
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

    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)

        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        //memulai dialog dan menampilkannya di layar
        mProgressDialog!!.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh -> {
                requestLocationData()
                true
            }else -> super.onOptionsItemSelected(item)

        }
        return super.onOptionsItemSelected(item)
    }



    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI(responseBody: WeatherResponse) {
        for (i in responseBody.weather.indices) {
            Log.i("Weather Name", responseBody.weather.toString())


            val tvCity: TextView = findViewById(R.id.tv_city_code)
            tvCity.text = responseBody.name + ", "
            val tvCountry: TextView = findViewById(R.id.tv_country)
            tvCountry.text = responseBody.sys.country
            val tvStatus: TextView = findViewById(R.id.tv_status)
            tvStatus.text = responseBody.weather[i].description
            val tvDegree: TextView = findViewById(R.id.tv_degree)
            tvDegree.text =
                responseBody.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            val tvMinTemp: TextView = findViewById(R.id.tv_min_temp)
            tvMinTemp.text = responseBody.main.temp_min.toString() + " min"
            val tvMaxTemp: TextView = findViewById(R.id.tv_max_temp)
            tvMaxTemp.text = responseBody.main.temp_max.toString() + " max"

            val tvSunriseTime: TextView = findViewById(R.id.tv_sunrise)
            tvSunriseTime.text = unixTime(responseBody.sys.sunrise)
            val tvSunsetTime: TextView = findViewById(R.id.tv_sunset)
            tvSunsetTime.text = unixTime(responseBody.sys.sunset)

            val tvWind: TextView = findViewById(R.id.tv_wind)
            tvWind.text = responseBody.wind.speed.toString() + " miles/hour"
            val tvPressure: TextView = findViewById(R.id.pressure)
            tvPressure.text = responseBody.main.pressure.toString()
            val tvHumidity: TextView = findViewById(R.id.humidity)
            tvHumidity.text = responseBody.main.humidity.toString() + " per cent"
            val tvVisibility: TextView = findViewById(R.id.tv_visibility)
            tvVisibility.text = responseBody.visibility.toString()

            val ivWeather: ImageView = findViewById(R.id.iv_weather_pictures)
            when(responseBody.weather[i].icon){
                "01d" -> ivWeather.setImageResource(R.drawable.sun)
                "02d" -> ivWeather.setImageResource(R.drawable.cloudy)
                "03d" -> ivWeather.setImageResource(R.drawable.cloud)
                "04d" -> ivWeather.setImageResource(R.drawable.cloud)
                "09d" -> ivWeather.setImageResource(R.drawable.rainyday)
                "10d" -> ivWeather.setImageResource(R.drawable.rainyday)
                "11d" -> ivWeather.setImageResource(R.drawable.thunder)
                "13d" -> ivWeather.setImageResource(R.drawable.snow)
                "01n" -> ivWeather.setImageResource(R.drawable.cloud)
                "02n" -> ivWeather.setImageResource(R.drawable.cloud)
                "03n" -> ivWeather.setImageResource(R.drawable.cloud)
                "10n" -> ivWeather.setImageResource(R.drawable.cloud)
                "11n" -> ivWeather.setImageResource(R.drawable.rainyday)
            }

        }
    }

    private fun getUnit(value: String): String? {
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}