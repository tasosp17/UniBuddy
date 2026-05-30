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
    private lateinit var btnUpcomingLectures: View
    private lateinit var btnNotifications: View

    // Data
    private lateinit var db: DatabaseReference
    private val userList  = mutableListOf<User>()
    private lateinit var adapter: UserAdapter

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
        btnNotifications = findViewById(R.id.btnNotifications)
        btnUpcomingLectures = findViewById(R.id.btnUpcomingLectures)

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


        db = FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app").getReference("users")

        //fetch data from firebase
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null){
            db.child(currentUid).child("isActive").onDisconnect().setValue(false)
            db.child(currentUid).get().addOnSuccessListener { snapshot ->

                //get online status
                val serverIsActive = snapshot.child("isActive").getValue(Boolean::class.java) ?: false
                wasOnline = serverIsActive

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
        updateVisibilityLabel() //for initial text

        btnNotifications.setOnClickListener {
            intent = Intent(this, NotificationsListActivity::class.java)
            startActivity(intent)
        }

        btnUpcomingLectures.setOnClickListener {
            intent = Intent(this, UpcomingLecturesActivity::class.java)
            startActivity(intent)
        }

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

    override fun onResume() {
        super.onResume()
        updateUserActiveStatus()
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

        //trap the android location security block
        if (ssid == "<unknown_ssid>"){
            Log.e("WIFI TEST", "Android is blocking SSID reading. Need Precise Location + GPS ON.")
            Toast.makeText(this, "Turn on GPS/Precise Location to detect Campus Wi-Fi", Toast.LENGTH_SHORT).show()
        }

        //retrieve list of SSIDs matching the registered university
        val allowedCampusSsids = getSsidsForUniversityId(mySsid)

        //compare current wifi name against any entry verified in the matching array
        val isAtUni = if (mySsid.isNotEmpty() && ssid != "<unknown ssid>"){
            allowedCampusSsids.contains(ssid)
        }else {false}

        //determine final status based on mode
        val finalStatus = when(visibilityMode){
            0 -> false //force offline
            else -> isAtUni
        }

        //if status changed to TRUE and weren't online before, send notif
        if (finalStatus && !wasOnline){
            if(visibilityMode == 1 || visibilityMode == 2){

                //--COOLDOWN--
                val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val lastNotifTime = prefs.getLong("LAST_NOTIF_TIME", 0L)
                val now = System.currentTimeMillis()

                //only notify if 60 minutes have passed since last notification
                if (now - lastNotifTime > 3_600_000){
                    notifyBestBuddies()
                    //save the exact time we sent this notif
                    prefs.edit().putLong("LAST_NOTIF_TIME", now).apply()
                }else {
                    Log.d("NOTIF_TEST", "Notification blocked! Still in the 1-hour cooldown period.")
                }
            }
        }

        //update local flag to avoid spamming
        wasOnline = finalStatus

        //update firebase
        val updates = mutableMapOf<String, Any>(
            "isActive" to finalStatus,
            "visibilityMode" to visibilityMode
        )

        if (finalStatus){
            updates["lastSeenOnCampus"] = System.currentTimeMillis()
        }

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

        //scan all users to see who has subscribed to us
        db.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {

                val myName = snapshot.child(currentUid).child("name").value?.toString() ?: "A Friend"
                val myAvatar = snapshot.child(currentUid).child("avatar").value?.toString() ?: ""
                //get our own best buddies list to check for mutual connections
                val myBestBuddiesNode = snapshot.child(currentUid).child("bestBuddies")

                //loop through database users
                for (userSnap in snapshot.children){
                    val otherUserId = userSnap.key ?: continue
                    if (otherUserId == currentUid) continue

                    //check if this user has ME in best buddies
                    val theyAddedMe = userSnap.child("bestBuddies").hasChild(currentUid)
                    if (theyAddedMe){
                        var shouldNotify = false

                        // --PRIVACY LOGIC--
                        if (visibilityMode == 2){
                            //Mode 2 (All): I am public. Anyone who added me gets the notif
                            shouldNotify = true
                        }else if (visibilityMode == 1){
                            //Mode 1 (Best Buddies): I am private. Only notify mutual connections
                            val iAddedThem = myBestBuddiesNode.hasChild(otherUserId)
                            if (iAddedThem){
                                shouldNotify = true
                            }
                        }

                        //send the notif if they passed the privacy check
                        if (shouldNotify){
                            val token = userSnap.child("fcmToken").value?.toString()
                            val timestamp = System.currentTimeMillis()

                            val followerHistoryRef = db.child(otherUserId).child("notificationHistory").push()
                            val statusNotif = AppNotification(
                                id = followerHistoryRef.key ?: "",
                                title = "UniBuddy",
                                body = "$myName is now online!",
                                type = "status",
                                timestamp = timestamp,
                                payloadId = currentUid, //pass ID so clicking the row opens user profile
                                senderName = myName,
                                senderAvatar = myAvatar
                            )
                            followerHistoryRef.setValue(statusNotif)

                            if (!token.isNullOrEmpty()){
                                Log.d("STATUS_NOTIF", "Found follower to notify: $otherUserId")
                                sendStatusFcmMessage(token, myName, currentUid, myAvatar)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendStatusFcmMessage(recipientToken: String, userName: String, myUid: String, myAvatar: String){
        val projectId = "uni-buddy-it2021084"
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        CoroutineScope(Dispatchers.IO).launch{
            val accessToken = FcmTokenSender.getAccessToken(applicationContext) ?: return@launch

            val dataPayload = JSONObject()
            dataPayload.put("type", "status")
            dataPayload.put("title", "UniBuddy")
            dataPayload.put("body", "$userName is now online!")
            dataPayload.put("userId", myUid)
            dataPayload.put("senderName", userName)
            dataPayload.put("senderAvatar", myAvatar)

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

