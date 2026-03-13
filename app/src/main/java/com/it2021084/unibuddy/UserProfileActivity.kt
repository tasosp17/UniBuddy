package com.it2021084.unibuddy

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.ByteArrayOutputStream

class UserProfileActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var etStatus: EditText
    private lateinit var btnSaveStatus: Button
    private lateinit var btnChangePhoto: Button

    private lateinit var btnBack: ImageButton
    private val PICK_IMAGE_REQUEST = 1

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app").reference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_profile)

        ivAvatar = findViewById(R.id.ivAvatar)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        etStatus = findViewById(R.id.etStatus)
        btnSaveStatus = findViewById(R.id.btnSaveStatus)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnBack = findViewById(R.id.btnBack)

        val user = auth.currentUser
        val uid = user?.uid ?: return

        //load basic user info
        tvName.text = user.displayName
        tvEmail.text = user.email

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

        //load existing status from database
        db.child("users").child(uid).child("status").get().addOnSuccessListener {
            etStatus.setText(it.value?.toString() ?: "") }

        //save new status
        btnSaveStatus.setOnClickListener {
            val newStatus = etStatus.text.toString().trim()
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val userNodeRef = db.child("users").child(uid)

            //node is guaranteed to exist because it is initialized at login
            userNodeRef.child("status").setValue(newStatus).addOnSuccessListener {
                Toast.makeText(this, "Status updated!", Toast.LENGTH_SHORT).show()
            }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update status.", Toast.LENGTH_SHORT).show()
                }
        }

        //change profile picture
        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }



        //back button functionality
        btnBack.setOnClickListener {
            finish()
        }

        setupBottomNav()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null){
            val imageUri: Uri = data.data!!
            uploadProfilePicture(imageUri)
        }
    }

    //upload profile picture max res 100x100
    private fun uploadProfilePicture(imageUri: Uri) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        try{
            //decode image from URI
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            //resize to max 100x100 px
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
            //convert to Base64 string
            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageBytes = baos.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            //save Base64 image in Realtime Database
            db.child("users").child(uid).child("avatar").setValue(base64Image)
                .addOnSuccessListener {
                    //show immediately
                    ivAvatar.setImageBitmap(resizedBitmap)
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update photo.", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(this, "Error processing image.", Toast.LENGTH_SHORT).show()
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
                    overridePendingTransition(0, 0) // no animation
                    true
                }

                R.id.nav_chats -> {
                    //open chats
                    val intent = Intent(this, ChatsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) // no animation
                    true
                }

                R.id.nav_more -> {
                    val intent = Intent(this, MoreActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) // no animation
                    true
                }

                else -> false

            }
        }
    }
}