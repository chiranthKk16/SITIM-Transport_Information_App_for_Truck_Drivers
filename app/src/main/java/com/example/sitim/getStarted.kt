package com.example.sitim

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sitim.databinding.ActivityGetStartedBinding
import com.google.firebase.database.*


class getStarted : AppCompatActivity() {

    private lateinit var binding: ActivityGetStartedBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var compReference: DatabaseReference
    private lateinit var truckReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        sharedPreferences = getSharedPreferences("sharedPrefFile", MODE_PRIVATE)
        compReference = FirebaseDatabase.getInstance().getReference("organisation")


        binding.submitCompany.setOnClickListener{
            getSharedPreferences("PREFERENCESITM", MODE_PRIVATE).edit().putBoolean("firstOpen" ,false).apply()

            val compName = binding.compnayNameEdittext.text?.trim().toString()
            val vehicleNum = binding.vehicleNumberEdittext.text?.trim().toString()

            if(!TextUtils.isEmpty(compName) && !TextUtils.isEmpty(vehicleNum)){

                compReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.hasChild(compName)) {

                            truckReference = FirebaseDatabase.getInstance().getReference("trucks/$compName")
                            truckReference.addListenerForSingleValueEvent(object: ValueEventListener{
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if(snapshot.hasChild(vehicleNum)) {
                                        val editor: SharedPreferences.Editor =
                                            sharedPreferences.edit()
                                        editor.putString("comp_name", compName)
                                        editor.putString("vehicle_num", vehicleNum)
                                        editor.apply()

                                        val intent =
                                            Intent(applicationContext, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }else{
                                        Toast.makeText(applicationContext, "No assigned jobs for you", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(applicationContext, "No assigned jobs for you", Toast.LENGTH_SHORT).show()
                                }

                            })
                        }else{
                            Toast.makeText(applicationContext, "Company doesn't exist", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(applicationContext, "Company doesn't exist", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}