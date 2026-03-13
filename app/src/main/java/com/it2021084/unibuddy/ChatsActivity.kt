package com.it2021084.unibuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlin.concurrent.timer

class ChatsActivity: AppCompatActivity() {

    private lateinit var recyclerViewChats: RecyclerView
    private val chatList = mutableListOf<ChatItem>()
    private lateinit var adapter: ChatsAdapter
    private val masterChatList = mutableListOf<ChatItem>() //storage
    private val displayChatList = mutableListOf<ChatItem>() //display
    private val currentUserId = FirebaseAuth.getInstance().uid!!
    private lateinit var dbRef : DatabaseReference
    private val lastSeenMap = mutableMapOf<String, Long>() //store the last time user opened chats

    //buttons
    private lateinit var btnAll: Button
    private lateinit var btnDMs: Button
    private lateinit var btnGroups: Button
    private lateinit var btnCourseGroups: Button

    //track current filter(0=All, 1=DMs, 2=Groups)
    private var currentFilterMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        //initialize views
        recyclerViewChats = findViewById(R.id.recyclerViewChats)
        btnAll = findViewById(R.id.btnAll)
        btnDMs = findViewById(R.id.btnDMs)
        btnGroups = findViewById(R.id.btnGroups)
        btnCourseGroups = findViewById(R.id.btnCourseGroups)

        recyclerViewChats.layoutManager = LinearLayoutManager(this)

        //initialize adapter with DISPLAY list
        adapter = ChatsAdapter(displayChatList) { openChat(it) }
        recyclerViewChats.adapter = adapter

        dbRef = FirebaseDatabase.getInstance().reference //database reference

        setupFilterButtons()

        //start listening to "Last Seen" data first
        listenToLastSeenData()

        setupBottomNav()

        val fab = findViewById<FloatingActionButton>(R.id.fabNewMessage)
        fab.setOnClickListener {
            openNewChatFragment()
        }

        // Handle back press for fragment
        onBackPressedDispatcher.addCallback(this) {
            val container = findViewById<FrameLayout>(R.id.fragmentContainer)
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                container.postDelayed({
                    container.visibility = View.GONE
                    bottomNav.visibility = View.VISIBLE

                    //restore new chat button
                    fab.visibility = View.VISIBLE

                }, 150)
            } else {
                // no fragment in back stack, finish activity
                finish()
            }
        }

    }

    //listen to when user last opened chats
    private fun listenToLastSeenData(){
        dbRef.child("users").child(currentUserId).child("lastSeen")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    lastSeenMap.clear()
                    for (child in snapshot.children){
                        val chatId = child.key ?: continue
                        val timestamp = child.value.toString().toLongOrNull() ?: 0L
                        lastSeenMap[chatId] = timestamp
                    }
                    //once we have the "read" times, we can load chats
                    loadChats()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    //navigation menu
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_chats
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
                    //already here
                    true
                }

                R.id.nav_more -> {
                    //open more
                    val intent = Intent(this, MoreActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) //no animation
                    true
                }

                else -> false

            }
        }
    }

    //load list of chats
    private fun loadChats(){
        dbRef.child("chats").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                masterChatList.clear() //clear master list
                for (chatSnapshot in snapshot.children){
                    val chatId = chatSnapshot.key ?: continue
                    //get list of members
                    val members = chatSnapshot.child("members").children.mapNotNull { it.getValue(String::class.java) }
                    if(!members.contains(currentUserId)) continue //skip this chat if user isn't included
                    val isGroup = chatSnapshot.child("isGroup").getValue(Boolean::class.java) == true
                    //get groupchat's name
                    val groupName = chatSnapshot.child("groupName").getValue(String::class.java)
                    val groupAvatar = chatSnapshot.child("groupAvatar").getValue(String::class.java)

                    //find last message
                    val msgSnap = chatSnapshot.child("messages")
                    var lastMsg: Message? = null
                    for (m in msgSnap.children){
                        val mm = m.getValue(Message::class.java) ?: continue
                        if (lastMsg == null || mm.timestamp > lastMsg!!.timestamp){
                            lastMsg = mm
                        }
                    }
                    //if it's private chat with no messages, skip it
                    if (lastMsg == null && !isGroup) continue

                    //check "Unread" status
                    //default last seen is 0 if never opened
                    val myLastSeen = lastSeenMap[chatId] ?: 0L
                    val lastMsgTime = lastMsg?.timestamp ?: 0L
                    val isMyMessage = lastMsg?.senderId == currentUserId

                    //if message is newer than when user last saw the chat, make it unread
                    val isUnread = !isMyMessage && (lastMsgTime > myLastSeen)

                    if (isGroup) loadGroupChatItem(chatId, members, lastMsg, groupName, groupAvatar, isUnread)
                    else loadPrivateChatItem(chatId, members, lastMsg, isUnread)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    //when user taps a chat
    private fun openChat(chat: ChatItem){
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chat.chatId)
        intent.putExtra("isGroup", chat.isGroup)
        intent.putStringArrayListExtra("memberIds", ArrayList(chat.memberIds))
        startActivity(intent)
    }

    //open new chat fragment
    private fun openNewChatFragment(){
        //hide bottom navigation menu when fragment is open
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.visibility = View.GONE
        val fab = findViewById<FloatingActionButton>(R.id.fabNewMessage)
        fab.visibility = View.GONE

        val container = findViewById<FrameLayout>(R.id.fragmentContainer)
        container.visibility = View.VISIBLE //show container

        val fragment = NewChatFragment()

        supportFragmentManager.beginTransaction().setCustomAnimations(
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right,
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right,
        )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null).commit()
    }

    private fun loadPrivateChatItem(chatId: String, members: List<String>, lastMessage: Message?, isUnread: Boolean){
        val otherId = members.first{it != currentUserId}

        dbRef.child("users").child(otherId).get().addOnSuccessListener { snap ->
            val name = snap.child("name").value?.toString() ?: "Unknown"
            val avatar = snap.child("avatar").value?.toString()

            val item = ChatItem(
                    chatId = chatId,
                    memberIds = members,
                    username = name,
                    avatar = avatar,
                    lastMessage = lastMessage!!.message,
                    timestamp = lastMessage.timestamp,
                    isGroup = false,
                    isUnread = isUnread //pass unread status
            )

            //remove duplicates if the item was already added (async safety)
            masterChatList.removeAll { it.chatId == chatId }
            masterChatList.add(item)

            //reapply filter to update UI
            applyFilter()
        }

    }

    private fun loadGroupChatItem(chatId: String, members: List<String>, lastMessage: Message?, groupName: String?, groupAvatar: String?, isUnread: Boolean){
        //logic to determine what text to show
        val displayText = lastMessage?.message ?: "No messages yet"
        val displayTime = lastMessage?.timestamp ?: System.currentTimeMillis()

        //if there is a proper groupName, use it
        //if not, list member names, besides user's
        if (!groupName.isNullOrEmpty()){
            val item = ChatItem(
                chatId = chatId,
                memberIds = members,
                username = groupName,
                avatar = groupAvatar,
                lastMessage = displayText,
                timestamp = displayTime,
                isGroup = true,
                isUnread = isUnread //pass unread status
            )
            addChatToMasterList(item)
        }
        else {
            dbRef.child("users").get().addOnSuccessListener { snap ->
                val names = members.filter { it != currentUserId }.mapNotNull { uid ->
                    snap.child(uid).child("name").getValue(String::class.java)
                }

                val item = ChatItem(
                    chatId = chatId,
                    memberIds = members,
                    username = names.joinToString(", "),
                    avatar = groupAvatar,
                    lastMessage = lastMessage!!.message,
                    timestamp = lastMessage.timestamp,
                    isGroup = true,
                    isUnread = isUnread // Pass unread status
                )
                addChatToMasterList(item)
            }
        }
    }

    //helper to avoid code duplication and async issues
    private fun addChatToMasterList(item: ChatItem){
        masterChatList.removeAll { it.chatId == item.chatId }
        masterChatList.add(item)
        //reapply filter to update UI
        applyFilter()
    }

    private fun setupFilterButtons(){
        val activeColor = getColor(R.color.purple_200)
        val inactiveColor = android.graphics.Color.parseColor("#E0E0E0")
        val activeText = android.graphics.Color.WHITE
        val inactiveText = android.graphics.Color.DKGRAY

        fun updateButtons(mode: Int){
            currentFilterMode = mode

            //reset all styles
            btnAll.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnDMs.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnGroups.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)
            btnCourseGroups.backgroundTintList = android.content.res.ColorStateList.valueOf(inactiveColor)

            btnAll.setTextColor(inactiveText)
            btnDMs.setTextColor(inactiveText)
            btnGroups.setTextColor(inactiveText)
            btnCourseGroups.setTextColor(inactiveText)

            //set active style
            when(mode){
                0 -> { //all
                    btnAll.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
                    btnAll.setTextColor(activeText)
                }
                1 -> { //DMs
                    btnDMs.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
                    btnDMs.setTextColor(activeText)
                }
                2 -> { //groups
                    btnGroups.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
                    btnGroups.setTextColor(activeText)
                }
                3 -> { //course groups
                    btnCourseGroups.backgroundTintList = android.content.res.ColorStateList.valueOf(activeColor)
                    btnCourseGroups.setTextColor(activeText)
                }
            }
            //apply filter to the list
            applyFilter()

        }
        btnAll.setOnClickListener { updateButtons(0) }
        btnDMs.setOnClickListener { updateButtons(1) }
        btnGroups.setOnClickListener { updateButtons(2) }
        btnCourseGroups.setOnClickListener { updateButtons(3) }

        updateButtons(0) //active on startup
    }

    //core filtering logic
    private fun applyFilter(){
        displayChatList.clear()
        //creates list of all known course IDs
        val courseIds = CourseCatalog.allCourses.map{it.id}

        when(currentFilterMode){
            0 -> displayChatList.addAll(masterChatList) //all
            1 -> displayChatList.addAll(masterChatList.filter{ !it.isGroup })//DMs
            2 -> displayChatList.addAll(masterChatList.filter{ it.isGroup && !courseIds.contains(it.chatId)})//groups
            3 -> displayChatList.addAll(masterChatList.filter{ it.isGroup && courseIds.contains(it.chatId)})// course groups
        }
        //sort unread first, then by timestamp
        displayChatList.sortWith(
            compareByDescending<ChatItem> { it.isUnread }
                .thenByDescending { it.timestamp }
        )
        adapter.notifyDataSetChanged()
    }

}