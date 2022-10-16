package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.android.gms.location.LocationRequest;
import android.location.Location
import android.location.LocationManager
//import android.location.LocationRequest
import android.net.Uri
import android.net.Uri.fromParts
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.modals.weatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private var customProgressDialog:Dialog?=null
    private lateinit var mFusedLocationClient:FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        mFusedLocationClient=LocationServices.getFusedLocationProviderClient((this))

        if(!isLocationEnabled()){
            Toast.makeText(this, "turn on GPS", Toast.LENGTH_SHORT).show()
            val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object:MultiplePermissionsListener{
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if(report!!.areAllPermissionsGranted()){
                            requestLocationData()
                        }

                        if(report.isAnyPermissionPermanentlyDenied){
                            Toast.makeText(this@MainActivity, "you've denied location", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }

                }).onSameThread()
                .check()
        }
    }

    private val mLocationCallback=object:LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? =locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("current latitude","$latitude")
            val longitude = mLastLocation?.longitude
            Log.i("current latitude","$longitude")
            if (longitude != null) {
                if (latitude != null) {
                    getLocationWeatherDetails(latitude,longitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){
        val mLocationRequest= LocationRequest()
        mLocationRequest.priority=LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,mLocationCallback, Looper.myLooper()
        )
    }

    private fun isLocationEnabled():Boolean{
        val locationManager: LocationManager=
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this)
            .setMessage("you've turned off permissions")
            .setPositiveButton("GO to settings")
            { _,_->
                try {
                    val intent=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri= Uri.fromParts("package",packageName,null)
                    intent.data=uri
                    startActivity(intent)
                }catch (e:ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("cancel"){
                    dialog,_->dialog.dismiss()
            }.show()
    }

    private fun getLocationWeatherDetails(latitude:Double,longitude:Double){
        if(Constants.isNetworkAvailable(this)){
            val retrofit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
            val service: WeatherService =retrofit.create<WeatherService>(WeatherService::class.java)

            val listCall: Call<weatherResponse> = service.getWeather(latitude,longitude,Constants.METRIC_UNIT,Constants.APP_Id)

            showProgressDialog()
            listCall.enqueue(object : Callback<weatherResponse> {
                override fun onResponse(
                    call: Call<weatherResponse>,
                    response: Response<weatherResponse>
                ) {
                    cancelProgressDialog()
                    if(response!!.isSuccessful){
                        val weatherList: weatherResponse? =response.body()
                        Log.i("response result","$weatherList")
                    }else{
                        val rc=response.code()
                        when(rc){
                            400->{
                                Log.e("error 400","not found")
                            }
                            else -> {
                                Log.e("error","generic error")
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<weatherResponse>, t: Throwable) {
                    cancelProgressDialog()
                    Log.e("errorrrr", t.message.toString())
                }

            })


        }else{
            Toast.makeText(this@MainActivity, "no internet connection", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showProgressDialog(){
        customProgressDialog= Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.circular_progress_dialog)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(customProgressDialog!=null){
            customProgressDialog?.dismiss()
            customProgressDialog=null
        }
    }



}