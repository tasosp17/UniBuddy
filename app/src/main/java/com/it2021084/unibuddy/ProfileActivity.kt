package com.it2021084.unibuddy

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity: AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvStatus: TextView
    private lateinit var ivActiveStatus: ImageView
    private lateinit var btnSendMessage: MaterialButton
    private lateinit var btnBack: ImageButton

    //recycler view for shared courses
    private lateinit var rvSharedCourses: RecyclerView
    private lateinit var sharedCoursesAdapter: SharedCoursesAdapter
    private val sharedCourseList = mutableListOf<Course>()

    private val db = FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app").reference
    private val currentUserId = FirebaseAuth.getInstance().uid!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)

        ivAvatar = findViewById(R.id.ivAvatar)
        tvUsername = findViewById(R.id.tvUserName)
        tvStatus = findViewById(R.id.tvStatus)
        ivActiveStatus = findViewById(R.id.ivActiveStatus)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnBack = findViewById(R.id.btnBack)
        rvSharedCourses = findViewById(R.id.rvSharedCourses)

        //get the userId passed from MainActivity
        val userId = intent.getStringExtra("userId")
        if (userId == null) {
            Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //setup recycler view
        setupSharedCoursesRecycler()

        //load profile data
        loadUserProfile(userId)

        //load shared courses
        loadSharedCourses(userId)

        //send message button
        btnSendMessage.setOnClickListener {
            val memberIds = arrayListOf(FirebaseAuth.getInstance().uid!!, userId)
            val chatId = memberIds.sorted().joinToString("_")
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("chatId", chatId)
                putExtra("isGroup", false)
                putStringArrayListExtra("memberIds", memberIds)
            }
            startActivity(intent)
        }


        //back button functionality
        btnBack.setOnClickListener {
            finish()
        }

        setupBottomNav()
    }

    private fun setupSharedCoursesRecycler(){
        sharedCoursesAdapter = SharedCoursesAdapter(sharedCourseList){course ->
            //on click: go to course chat
            val intent = Intent(this, ChatActivity::class.java).apply{
                putExtra("chatId", course.id) //course ID is chat ID
                putExtra("isGroup", true)
            }
            startActivity(intent)
        }
        rvSharedCourses.layoutManager = LinearLayoutManager(this)
        rvSharedCourses.adapter = sharedCoursesAdapter
    }

    private fun loadUserProfile(userId: String){
        db.child("users").child(userId).get().addOnSuccessListener { snapshot ->
            val userName = snapshot.child("name").value?.toString() ?: "User"
            val status = snapshot.child("status").value?.toString() ?: ""
            val isActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: false
            val avatar = snapshot.child("avatar").value?.toString()

            tvUsername.text = userName
            tvStatus.text = status

            //show active/inactive status
            val activeIcon = if (isActive) R.drawable.status_circle_active else R.drawable.status_circle_inactive
            ivActiveStatus.setImageResource(activeIcon)

            //load avatar from Base64 if it exists
            if (!avatar.isNullOrEmpty()){
                try{
                    val decodedBytes = Base64.decode(avatar, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0 , decodedBytes.size)
                    ivAvatar.setImageBitmap(bitmap)
                } catch (e: Exception){
                    e.printStackTrace()
                    Glide.with(this)
                        .load(R.drawable.ic_profile_placeholder)
                        .apply(RequestOptions.circleCropTransform())
                        .into(ivAvatar)
                }
            } else {
                //fallback placeholder
                Glide.with(this)
                    .load(R.drawable.ic_profile_placeholder)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivAvatar)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSharedCourses(userId: String){
        //get current user's courses
        db.child("users").child(currentUserId).child("enrolledCourses").get().addOnSuccessListener { mySnap ->
            val myCourses = mutableListOf<String>()
            for (child in mySnap.children){
                child.key?.let{myCourses.add(it)}
            }

            //get profile user's courses
            db.child("users").child(userId).child("enrolledCourses").get().addOnSuccessListener { otherSnap ->
                val commonCourseIds = mutableListOf<String>()
                for (child in otherSnap.children){
                    val courseId = child.key
                    //find intersection
                    if (courseId != null && myCourses.contains(courseId)){
                        commonCourseIds.add(courseId)
                    }
                }
                //map IDs to course objects using catalog
                sharedCourseList.clear()
                val allCatalogCourses = CourseCatalog.allCourses

                for (id in commonCourseIds){
                    val courseObj = allCatalogCourses.find {it.id == id}
                    if (courseObj != null){ sharedCourseList.add(courseObj) }
                }

                sharedCoursesAdapter.notifyDataSetChanged()

                //update title if empty
                val titleView = findViewById<TextView>(R.id.tvSharedCoursesTitle)
                if (sharedCourseList.isEmpty()){
                    titleView.text = "No Shared Courses"
                }else{
                    titleView.text = "Shared Courses"
                }
            }
        }
    }
    //navigation menu
    private fun setupBottomNav(){
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId){
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