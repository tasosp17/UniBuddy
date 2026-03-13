package com.it2021084.unibuddy

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Button
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView
import android.widget.Toast
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.FirebaseDatabase
import android.widget.TextView
import com.bumptech.glide.Glide


class MoreActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var btnCourseSelection: View
    private lateinit var btnSettings: View
    private lateinit var btnBestBuddies: View

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app").reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)

        tvUserName = findViewById(R.id.tvUserName)
        ivAvatar = findViewById(R.id.ivAvatar)
        btnCourseSelection = findViewById(R.id.btnCourseSelection)
        btnSettings = findViewById(R.id.btnSettings)
        btnBestBuddies = findViewById(R.id.btnBestBuddies)

        loadUserData()

        //go to course selection
        btnCourseSelection.setOnClickListener {
            val intent = Intent(this, CourseSelectionActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnBestBuddies.setOnClickListener {
            val intent = Intent(this, BestBuddiesActivity::class.java)
            startActivity(intent)
        }

        //logout button
        val logoutButton = findViewById<Button>(R.id.btnSignOut)
        logoutButton.setOnClickListener { logoutUser() }

        //go to ProfileView when tapping profile card
        val profileCard = findViewById<MaterialCardView>(R.id.cardProfile)
        profileCard.setOnClickListener {
            //placeholder , add ProfileActivity
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        setupBottomNav()

    }

    private fun loadUserData() {
        val user = auth.currentUser
        val uid = user?.uid ?: return

        //load name and profile photo
        db.child("users").child(uid).get().addOnSuccessListener { snapshot ->
            val name = snapshot.child("name").value?.toString() ?: user.displayName ?: "User"
            tvUserName.text = name

            //load Base64 profile picture from Realtime Database
            db.child("users").child(uid).child("avatar").get().addOnSuccessListener { snapshot ->
                val base64String = snapshot.value?.toString()
                if (!base64String.isNullOrEmpty()) {
                    val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0 , imageBytes.size)
                    ivAvatar.setImageBitmap(bitmap)
                }else {
                    ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        }
    }

    //logout function
    private fun logoutUser(){
        //sign out from firebase
        FirebaseAuth.getInstance().signOut()

        //sign out from google
        val googleSignInClient = GoogleSignIn.getClient(
            this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        )
        googleSignInClient.signOut().addOnCompleteListener{
            //after signing out, go back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    //navigation menu
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_more
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    //show home screen
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) //no animation
                    true
                }

                R.id.nav_chats -> {
                    //open chats
                    val intent = Intent(this, ChatsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) //no animation
                    true
                }

                R.id.nav_more -> {
                    //already here
                    true
                }

                else -> false

            }
        }
    }
}