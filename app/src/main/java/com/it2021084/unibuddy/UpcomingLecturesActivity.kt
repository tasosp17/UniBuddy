package com.it2021084.unibuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import kotlin.text.clear

class UpcomingLecturesActivity: AppCompatActivity() {

    private lateinit var rvUpcoming: RecyclerView
    private lateinit var tvNoCourses: TextView
    private lateinit var db: DatabaseReference
    private val upcomingList = mutableListOf<UpcomingLectureItem>()
    private lateinit var upcomingAdapter: UpcomingLecturesAdapter
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upcoming_lectures)

        db = FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app").getReference("users")

        rvUpcoming = findViewById(R.id.rvUpcomingLectures)
        tvNoCourses = findViewById(R.id.tvNoCourses)
        btnBack = findViewById(R.id.btnBack)

        // upcoming lectures recyclerview
        upcomingAdapter = UpcomingLecturesAdapter(upcomingList)
        rvUpcoming.layoutManager = LinearLayoutManager(this)
        rvUpcoming.adapter = upcomingAdapter

        loadUpcomingLectures()
        setupBottomNav()

        btnBack.setOnClickListener { finish() }
    }

    private fun loadUpcomingLectures(){
        val currentUid = FirebaseAuth.getInstance().uid ?: return

        //listen to user's enrolled courses
        db.child(currentUid).child("enrolledCourses")?.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                upcomingList.clear()
                val enrolledIds = mutableListOf<String>()

                for (child in snapshot.children){
                    child.key?.let {enrolledIds.add(it)}
                }

                if (enrolledIds.isEmpty()){
                    tvNoCourses.visibility = View.VISIBLE
                    rvUpcoming.visibility = View.GONE
                } else {
                    tvNoCourses.visibility = View.GONE
                    rvUpcoming.visibility = View.VISIBLE

                    calculateUpcomingLectures(enrolledIds)

                    //schedule alarms
                    CourseAlarmScheduler.scheduleAlarmsForCourses(this@UpcomingLecturesActivity, enrolledIds.toSet())
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun calculateUpcomingLectures(enrolledIds: List<String>){
        val now = System.currentTimeMillis()

        //loop through enrolled courses
        for (courseId in enrolledIds){
            val course = CourseCatalog.allCourses.find { it.id == courseId } ?: continue

            //loop through the schedule of each course
            for (schedule in course.schedule){
                val nextTime = getNextOccurrence(schedule.dayOfWeek, schedule.hour, schedule.minute)
                upcomingList.add(UpcomingLectureItem(course.name, nextTime))
            }
        }
        //sort by earliest first
        upcomingList.sortBy { it.timestamp }
        upcomingAdapter.notifyDataSetChanged()
    }

    //helper to calculate next class time
    private fun getNextOccurrence(targetDay: Int, targetHour: Int, targetMinute: Int): Long{
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_WEEK) // sun=1, mon= 2...

        //calculate difference
        var daysDiff = targetDay - currentDay

        //logic to handle scheduling
        if (daysDiff < 0){
            //day has passed this week, add 7 days to move to next
            daysDiff += 7
        } else if (daysDiff == 0){
            //same day, check time
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val currentMin = cal.get(Calendar.MINUTE)

            if (currentHour > targetHour || (currentHour == targetHour && currentMin >= targetMinute)){
                //lecture finished for today, move to next week
                daysDiff += 7
            }
        }

        cal.add(Calendar.DAY_OF_YEAR, daysDiff)
        cal.set(Calendar.HOUR_OF_DAY, targetHour)
        cal.set(Calendar.MINUTE, targetMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }

    //navigation menu
    private fun setupBottomNav(){
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId){
                R.id.nav_home -> {
                    //show home screen
                    true
                }
                R.id.nav_chats -> {
                    //open chats
                    val intent = Intent(this, ChatsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0,0) // no animation
                    true
                }
                R.id.nav_more -> {
                    val intent = Intent(this, MoreActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0,0) // no animation
                    true
                }
                else -> false

            }
        }
    }
}