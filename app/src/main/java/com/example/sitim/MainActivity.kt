package com.example.sitim

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.TaskStackBuilder.create
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.media.audiofx.Equalizer.Settings
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sitim.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var prodList: ArrayList<String>
    private lateinit var dbRef: DatabaseReference
    private lateinit var recyclerAdapter: productAdapter
    private lateinit var truckReference: DatabaseReference
    private lateinit var compReference: DatabaseReference
    private lateinit var detReference: DatabaseReference
    private lateinit var compName: String
    private lateinit var vehicleNum: String
    private var destLong: Double = 0.00
    private var destLat: Double = 0.00
    private lateinit var currLong: String
    private lateinit var currLat: String
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        val firstOpen =
            getSharedPreferences("PREFERENCESITM", MODE_PRIVATE).getBoolean("firstOpen", true)

        if (firstOpen) {
            //FirebaseDatabase.getInstance().setPersistenceEnabled(true)
            val intent = Intent(this, getStarted::class.java)
            startActivity(intent)
            finish()
        } else {


            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            sharedPreferences = getSharedPreferences("sharedPrefFile", MODE_PRIVATE)

            getCurrentLocation()

            binding.mapView.setOnClickListener {
                val lat = destLat
                val longitude = destLong
                val mapsUrl = Uri.parse("google.navigation:q=$lat,$longitude")
                val mapIntent = Intent(Intent.ACTION_VIEW, mapsUrl)
                mapIntent.setPackage("com.google.android.apps.maps")

                startActivity(mapIntent)
            }

            binding.finishJob.setOnClickListener {
                getSharedPreferences("PREFERENCESITM", MODE_PRIVATE).edit()
                    .putBoolean("firstOpen", true).apply()
                val editor = sharedPreferences.edit()
                editor.clear()
                editor.apply()



                detReference = FirebaseDatabase.getInstance().getReference("trucks/$compName/${vehicleNum}/details")
                detReference.child("status").setValue("Delivered")



                val intent = Intent(this, getStarted::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    fun getData(){
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        compName = sharedPreferences.getString("comp_name", "roxconn").toString()
        vehicleNum = sharedPreferences.getString("vehicle_num", "KA05MA1234").toString()
        //Toast.makeText(this, "$compName and $vehicleNum", Toast.LENGTH_SHORT).show()

        detReference = FirebaseDatabase.getInstance().getReference("trucks/$compName/$vehicleNum/details")

        compReference = FirebaseDatabase.getInstance().getReference("organisation/$compName")


        detReference.child("dest_long").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                destLong = snapshot.value.toString().toDouble()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

        detReference.child("dest_lat").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                destLat = snapshot.value.toString().toDouble()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        detReference.child("dest_address").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.destinationAddress.text = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        detReference.child("vehicle_num").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.vehiclePlate.text = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

        binding.employerName.text = compName

        compReference.child("address").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.employerAddress.text = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

        compReference.child("phone_number").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.employerPhone.text = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

        binding.productsRecycler.layoutManager = LinearLayoutManager(this)
        binding.productsRecycler.setHasFixedSize(true)

        prodList = arrayListOf<String>()

        dbRef = FirebaseDatabase.getInstance().getReference("trucks/$compName/$vehicleNum/goods/")
        dbRef.keepSynced(true)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                prodList.clear()
                if (snapshot.exists()) {
                    for (prodSnap in snapshot.children) {
                        val prodData = prodSnap.value.toString()
                        prodList.add(prodData)
                    }
                    recyclerAdapter = productAdapter(prodList)
                    binding.productsRecycler.adapter = recyclerAdapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun getCurrentLocation(){
//        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        getData()

        detReference = FirebaseDatabase.getInstance().getReference("trucks/$compName/$vehicleNum/details")
        if(checkPermission()){
             if(isLocationEnabled()){
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task->
                    val location: Location?=task.result
                    if(location == null){
                        Toast.makeText(this, "Null received", Toast.LENGTH_SHORT).show()
                    }else{
                        detReference.child("curr_lat").setValue(location.latitude)
                        detReference.child("curr_long").setValue(location.longitude)
                        //Toast.makeText(this, "$currLat and $currLong", Toast.LENGTH_SHORT).show()
                    }
                }
             }else{
                 Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                 val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                 startActivity(intent)
             }
        }else{
            requestPermission()
        }
    }

    companion object{
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private fun checkPermission():Boolean{
        if(ActivityCompat.checkSelfPermission(applicationContext,  android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation()
            }else{
                requestPermission()
            }
        }
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager:LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }
}

