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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NotificationsListActivity: AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvNoNotifs: TextView
    private val historyList = mutableListOf<AppNotification>()
    private lateinit var notifAdapter: NotificationAdapter
    private val DB_URL = "https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications_list)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        rvHistory = findViewById(R.id.rvHistory)
        tvNoNotifs = findViewById(R.id.tvNoNotifs)

        rvHistory.layoutManager = LinearLayoutManager(this)
        notifAdapter = NotificationAdapter(historyList) { targetLog ->
            executeIntentRoutingMatrix(targetLog)
        }
        rvHistory.adapter = notifAdapter

        btnBack.setOnClickListener { finish() }

        // Execute data maintenance defensively before mapping updates
        enforceLedgerCapSize()
        syncLedgerFeed()
        setupBottomNav()
    }

    private fun enforceLedgerCapSize() {
        val currentUid = FirebaseAuth.getInstance().uid ?: return
        val dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("users").child(currentUid).child("notificationHistory")

        // Isolated Single Value event block prevents recursive loops entirely
        dbRef.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount > 10) {
                    val rawLogs = snapshot.children.mapNotNull { it.getValue(AppNotification::class.java) }.toMutableList()
                    rawLogs.sortBy { it.timestamp }

                    while (rawLogs.size > 10) {
                        val olderItem = rawLogs.removeAt(0)
                        dbRef.child(olderItem.id).removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun syncLedgerFeed(){
        val currentUid = FirebaseAuth.getInstance().uid ?: return
        val dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("users").child(currentUid).child("notificationHistory")

        dbRef.orderByChild("timestamp").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for(child in snapshot.children){
                    child.getValue(AppNotification::class.java)?.let { historyList.add(it) }
                }
                historyList.reverse()
                notifAdapter.notifyDataSetChanged()

                if (historyList.isEmpty()) {
                    tvNoNotifs.visibility = View.VISIBLE
                    rvHistory.visibility = View.GONE
                } else {
                    tvNoNotifs.visibility = View.GONE
                    rvHistory.visibility = View.VISIBLE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun executeIntentRoutingMatrix(log: AppNotification){
        val intent = when (log.type){
            "chat" -> Intent(this, ChatActivity::class.java).apply{
                putExtra("chatId", log.payloadId)
                putExtra("chatName", log.senderName)
            }
            "status" -> Intent(this, ProfileActivity::class.java).apply{
                putExtra("userId", log.payloadId)
            }
            "broadcast" -> Intent(this, BroadcastViewActivity::class.java).apply{
                putExtra("broadcast_body", log.payloadId)
            }
            "lecture" -> Intent(this, UpcomingLecturesActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_chats -> {
                    startActivity(Intent(this, ChatsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_more -> true
                else -> false
            }
        }
    }
}