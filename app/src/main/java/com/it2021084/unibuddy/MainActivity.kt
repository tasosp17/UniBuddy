package com.it2021084.unibuddy

import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.it2021084.unibuddy.ui.theme.UniBuddyTheme
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DatabaseError
import android.content.Context
import android.content.IntentFilter
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.slider.Slider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar


class MainActivity : AppCompatActivity() {

    //UI components
    private lateinit var rvUsers: RecyclerView
    private lateinit var sliderVisibility: Slider
    private lateinit var tvVisibilityLabel: TextView
    private lateinit var rvUpcoming: RecyclerView
    private lateinit var tvNoCourses: TextView

    // Data
    private lateinit var db: DatabaseReference
    private val userList  = mutableListOf<User>()
    private lateinit var adapter: UserAdapter
    private val upcomingList = mutableListOf<UpcomingLectureItem>()
    private lateinit var upcomingAdapter: UpcomingLecturesAdapter

    // Logic Variables
    private var wasOnline = false //prevent spamming if wifi flickers
    private var mySsid: String = ""
    private var visibilityMode = 2 // 0=None, 1=Best Buddies, 2=All
    private val bestBuddiesIds = mutableSetOf<String>()

    //broadcast receiver that listens for changes in WiFi connectivity
    private val wifiReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?){
            updateUserActiveStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //load cached data instantly
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        mySsid = prefs.getString("LOCAL_SSID", "") ?: ""
        visibilityMode = prefs.getInt("LOCAL_MODE", 2)

        // initialize UI
        sliderVisibility = findViewById(R.id.sliderVisibility)
        tvVisibilityLabel = findViewById(R.id.tvVisibilityLabel)
        rvUsers = findViewById(R.id.rvUsers)
        rvUpcoming = findViewById(R.id.rvUpcomingLectures)
        tvNoCourses = findViewById(R.id.tvNoCourses)

        //setup slider immediately so we don't wait for firebase
        sliderVisibility.value = visibilityMode.toFloat()
        updateVisibilityLabel()

        //setup slider listener
        sliderVisibility.addOnChangeListener { _, value, _ ->
            visibilityMode = value.toInt()
            updateVisibilityLabel()
            updateUserActiveStatus()
        }

        //setup broadcast receiver
        val intentFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(wifiReceiver, intentFilter)

        //users recycler view
        adapter = UserAdapter(userList) { user, view ->
            //handle click menu
            showUserPopupMenu(user, view)
        }
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = adapter
     //   val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        // rvUsers.addItemDecoration(divider)

        // upcoming lectures recyclerview
        upcomingAdapter = UpcomingLecturesAdapter(upcomingList)
        rvUpcoming.layoutManager = LinearLayoutManager(this)
        rvUpcoming.adapter = upcomingAdapter

        db = FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app").getReference("users")

        //fetch data from firebase
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null){
            db.child(currentUid).child("isActive").onDisconnect().setValue(false)
            db.child(currentUid).get().addOnSuccessListener { snapshot ->
                //fetch ssid
                val serverSsid = snapshot.child("ssid").value?.toString() ?: ""
                if (serverSsid.isNotEmpty()) mySsid = serverSsid

                //fetch visibilityMode
                val serverVisibilityMode = snapshot.child("visibilityMode").value?.toString()?.toIntOrNull() ?: 2
                if (serverVisibilityMode != null && serverVisibilityMode != visibilityMode){
                    visibilityMode = serverVisibilityMode
                    sliderVisibility.value = visibilityMode.toFloat()
                    updateVisibilityLabel()
                }

                //fetch my best buddies list
                fetchBestBuddies(currentUid){
                    //start listening for other users
                    listenForUsers(currentUid)
                }

                //and trigger an initial status update check
                updateUserActiveStatus()
            }
        }

        //get user token and save it to firebase
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if(task.isSuccessful){
                val token = task.result
                val currentUid = FirebaseAuth.getInstance().uid

                //save the token to user node
                if (currentUid != null && token != null){
                    db.child(currentUid).child("fcmToken").setValue(token)
                }
            }
        }

        setupBottomNav()
        loadUpcomingLectures()
        updateVisibilityLabel() //for initial text

        //request runtime permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU){
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        //ask for location access
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 102)
        }

    }

    //avoid memory leaks for broadcast receiver
    override fun onDestroy(){
        super.onDestroy()
        unregisterReceiver(wifiReceiver)
    }

    private fun updateVisibilityLabel(){
        val text = when(visibilityMode){
            0 -> "Visible to: None"
            1 -> "Visible to: Best Buddies"
            2 -> "Visible to: All"
            else -> "Visible to: All"
        }
        tvVisibilityLabel.text = text
    }

    //retrieve best buddies list of user
    private fun fetchBestBuddies(uid: String, onComplete: () -> Unit){
        db.child(uid).child("bestBuddies").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                bestBuddiesIds.clear()
                for(child in snapshot.children){
                    child.key?.let { bestBuddiesIds.add(it) }
                }
                onComplete() //proceed to load users
                //refresh user list
                if (userList.isNotEmpty()){
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    //check if user is connected to the university's wifi and update their status, also include toggle
    private fun updateUserActiveStatus() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        val ssid = info.ssid.replace("\"", "")

        Log.d("WIFI_TEST", "Current SSID: $ssid vs Target: $mySsid")

        //compare current wifi against the stored firebase SSID
        val isAtUni = if (mySsid.isNotEmpty()){
            ssid == mySsid
        }else {false}

        //determine final status based on mode
        val finalStatus = when(visibilityMode){
            0 -> false //force offline
            else -> isAtUni
        }

        //if status changed to TRUE and weren't online before, send notif
        if (finalStatus && !wasOnline){
            if(visibilityMode == 1 || visibilityMode == 2){
                notifyBestBuddies()
            }
        }

        //update local flag to avoid spamming
        wasOnline = finalStatus

        //update firebase
        val updates = mapOf(
            "isActive" to finalStatus,
            "visibilityMode" to visibilityMode
        )
        db.child(currentUserUid).updateChildren(updates)

        // Save my status locally so the Notification Service can check it
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("AM_I_ACTIVE", finalStatus)
            .putInt("LOCAL_MODE", visibilityMode)
            .apply()
    }

    //popup menu function
    private fun showUserPopupMenu(user: User, anchorView: View){
        val popupMenu = PopupMenu(this, anchorView)

        //add menu items
        popupMenu.menu.add("Send Message")
        popupMenu.menu.add("View Profile")

        //handling menu clicks
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Send Message" -> {
                    //open chat with selected user
                    val currentUid =
                        FirebaseAuth.getInstance().uid ?: return@setOnMenuItemClickListener true
                    val memberIds = arrayListOf(currentUid, user.uid)
                    val chatId = memberIds.sorted().joinToString("_")
                    val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("chatId", chatId)
                    putExtra("isGroup", false)
                    putStringArrayListExtra("memberIds", memberIds)

                    }
                    startActivity(intent)
                    true
                }
                "View Profile" -> {
                    //view profile
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("userId", user.uid) //pass clicked user's UID
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
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
                    CourseAlarmScheduler.scheduleAlarmsForCourses(this@MainActivity, enrolledIds.toSet())
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

    private fun notifyBestBuddies(){
        val currentUid = FirebaseAuth.getInstance().uid ?: return
        val myName = FirebaseAuth.getInstance().currentUser?.displayName ?: "A Friend"

        //look specifically at the bestBuddies node
        db.child(currentUid).child("bestBuddies").addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //loop through UIDs in the bestBuddies list
                for (child in snapshot.children){
                    val friendUid = child.key ?: continue //key is userId

                    //get friend's token
                    FirebaseDatabase.getInstance().getReference("users").child(friendUid).child("fcmToken").get().addOnSuccessListener { tokenSnap ->
                        val token = tokenSnap.value?.toString()
                        if(!token.isNullOrEmpty()){
                            //send status update
                            Log.d("STATUS_NOTIF", "Found buddy to notify: $friendUid")
                            sendStatusFcmMessage(token, myName, currentUid)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendStatusFcmMessage(recipientToken: String, userName: String, myUid: String){
        val projectId = "uni-buddy-it2021084"
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        CoroutineScope(Dispatchers.IO).launch{
            val accessToken = FcmTokenSender.getAccessToken(applicationContext) ?: return@launch

            val dataPayload = JSONObject()
            dataPayload.put("type", "status")
            dataPayload.put("title", "UniBuddy")
            dataPayload.put("body", "$userName is now online!")
            dataPayload.put("userId", myUid)

            val messagePayload = JSONObject()
            messagePayload.put("token", recipientToken)
            messagePayload.put("data", dataPayload)

            val rootPayload = JSONObject()
            rootPayload.put("message", messagePayload)

            val request = object: JsonObjectRequest(Method.POST, fcmUrl, rootPayload,
                Response.Listener {Log.d("FCM", "Status sent!")},
                Response.ErrorListener {error -> Log.e("FCM", "Error: $error")}
            ) {
                override fun getHeaders(): MutableMap<String, String>{
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer $accessToken"
                    headers["Content-Type"] = "application/json"
                    return headers
                }
            }
            withContext(Dispatchers.Main){
                Volley.newRequestQueue(this@MainActivity).add(request)
            }
        }
    }

    //listener to only show users with matching SSID
    private fun listenForUsers(currentUid: String){
        db.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (child in snapshot.children){
                    val user = child.getValue(User::class.java)
                    user?.let { remoteUser ->
                        //filter user (me) and same SSID
                        if(remoteUser.uid != currentUid && remoteUser.ssid == mySsid) {
                            //visibility filter
                            if (remoteUser.visibilityMode == 0){
                                //always marked as inactive
                                remoteUser.isActive = false
                            } else if (remoteUser.visibilityMode == 1) {
                                val amIInTheirList = remoteUser.bestBuddies.containsKey(currentUid)
                                if (!amIInTheirList){
                                    remoteUser.isActive = false
                                }
                            }
                            userList.add(remoteUser)
                            Log.d("MainActivity",  "Fetched user: ${remoteUser.name}")
                        }
                    }
                }
                userList.sortByDescending { it.isActive } //display active users first
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError){}
        })
    }

}

