package com.it2021084.unibuddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity: AppCompatActivity() {

    private lateinit var etSsid: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton
    private var isSsidSet = false //flag to track if ssid configuration exists

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        btnBack = findViewById<ImageButton>(R.id.btnBack)
        val switchChat = findViewById<Switch>(R.id.switchChatNotifs)
        val switchLectures = findViewById<Switch>(R.id.switchLectureNotifs)
        etSsid = findViewById(R.id.etSsid)
        btnSave = findViewById(R.id.btnSaveSsid)

        val currentUid = FirebaseAuth.getInstance().uid!!
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid)

        //check firebase for existing SSID
        dbRef.child("ssid").get().addOnSuccessListener { snapshot ->
            val ssid = snapshot.value?.toString()

            if (!ssid.isNullOrEmpty()){
                etSsid.setText(ssid)
                isSsidSet = true
            }else{
                //ssid is empty, force user to set it
                isSsidSet = false
                lockUI()
            }
        }

        //save button logic
        btnSave.setOnClickListener {
            val inputSsid = etSsid.text.toString().trim()

            if (inputSsid.isEmpty()){
                Toast.makeText(this, "SSID cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dbRef.child("ssid").setValue(inputSsid).addOnSuccessListener {
                Toast.makeText(this, "Network Saved!", Toast.LENGTH_SHORT).show()
                if (!isSsidSet){
                    //if this was the first time setup, redirect to main activity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }else {
                    //just an update, unlock UI
                    unlockUI()
                }
                isSsidSet = true
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to save.", Toast.LENGTH_SHORT).show()
            }
        }

        //shared preferences to save state
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        //setup notifications
        switchChat.isChecked = sharedPrefs.getBoolean("NOTIF_CHAT", true)
        switchChat.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("NOTIF_CHAT", isChecked).apply()
        }

        switchLectures.isChecked = sharedPrefs.getBoolean("NOTIF_LECTURES", true)
        switchLectures.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("NOTIF_LECTURES", isChecked).apply()
        }

        btnBack.setOnClickListener {
            //only allow back if SSID is valid
            if (isSsidSet) finish()
        }

        //intercept system back button
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if (isSsidSet){
                    finish()
                }else {
                    Toast.makeText(
                        this@SettingsActivity, "Please configure your University WiFI first.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    //helper to lock the user in this screen
    private fun lockUI(){
        btnBack.visibility = View.INVISIBLE //hide back arrow
        Toast.makeText(this, "Welcome! Please set your University WiFi name to continue.", Toast.LENGTH_SHORT).show()
    }

    //helper to restore navigation
    private fun unlockUI(){
        btnBack.visibility = View.VISIBLE
    }
}