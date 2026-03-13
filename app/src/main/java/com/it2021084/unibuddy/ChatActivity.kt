package com.it2021084.unibuddy

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.snap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ChatActivity: AppCompatActivity() {

    //UI elements
    private lateinit var btnBack: ImageButton
    private lateinit var tvChatname: TextView
    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton

    // adapter for the recyclerview that displays messages
    private lateinit  var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()

    //firebase database reference & chat identifiers
    private lateinit var dbRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private var chatId = ""
    private var isGroup = false
    private var memberIds: List<String> = emptyList()

    private lateinit var currentUid: String
    private var currentUserName: String = ""
    private var currentUserAvatar: String? = null
    private var otherUserAvatar: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //initialize views
        btnBack = findViewById(R.id.btnBack)
        tvChatname = findViewById(R.id.tvChatName)
        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        currentUid = FirebaseAuth.getInstance().uid!!

        //disable send button if input is empty
        btnSend.isEnabled = false
        btnSend.alpha = 0.5f
        etMessage.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = s?.toString()?.trim()?.isNotEmpty() == true
                btnSend.isEnabled = hasText
                btnSend.alpha = if (hasText) 1f else 0.5f
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        //database references
        dbRef = FirebaseDatabase.getInstance().getReference("chats")
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        chatId = intent.getStringExtra("chatId") ?: ""
        isGroup = intent.getBooleanExtra("isGroup", false)
        memberIds = intent.getStringArrayListExtra("memberIds") ?: emptyList()

        if(chatId.isEmpty()){
            finish()
            return
        }

        //display header immediately from notification data
        val intentName = intent.getStringExtra("chatName")
        if (!intentName.isNullOrEmpty()){
            tvChatname.text = intentName
        }

        //update "last seen" timestamp so the chat appears "read"
        if (currentUid != null && chatId != null){
            val lastSeenRef = FirebaseDatabase.getInstance().getReference("users").child(currentUid).child("lastSeen").child(chatId)
            lastSeenRef.setValue(System.currentTimeMillis())
        }

        loadCurrentUser()
        setupChatHeader()

        setupRecycler()
        listenForMessages()

        //send button functionality
        btnSend.setOnClickListener { sendMessage() }

        //back button functionality
        btnBack.setOnClickListener { finish() }

        //open Chat Details when tapping header
        tvChatname.setOnClickListener {
            val intent = Intent(this, ChatDetailsActivity::class.java)
            //pass the crucial info to the details screen
            intent.putExtra("chatId", chatId)
            intent.putExtra("isGroup", isGroup)
            startActivity(intent)
        }
    }

    //check if user is currently viewing bottom of chat
    private fun isAtBottom(): Boolean {
        val layoutManager = recyclerViewChat.layoutManager as LinearLayoutManager
        val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
        return messageList.isNotEmpty() && lastVisible == messageList.size - 1
    }

    private fun listenForMessages(){
        dbRef.child(chatId).child("messages").orderByChild("timestamp").addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val wasAtBottom = isAtBottom()
                messageList.clear()
                for (postSnapshot in snapshot.children) {
                    val message = postSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        messageList.add(message)
                    }
                }
                messageAdapter.notifyDataSetChanged()
                if (wasAtBottom && messageList.isNotEmpty()){
                    recyclerViewChat.post{
                        recyclerViewChat.smoothScrollToPosition(messageList.size - 1)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    //load sender info
    private fun loadCurrentUser() {
        usersRef.child(currentUid).get().addOnSuccessListener { snap ->
            currentUserName = snap.child("name").value?.toString() ?: ""
            currentUserAvatar = snap.child("avatar").value?.toString()
        }
    }

    //recyclerview setup
    private fun setupRecycler() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true

        messageAdapter = MessageAdapter(
            messageList,
            isGroup,
            otherUserAvatar
        )

        recyclerViewChat.layoutManager = layoutManager
        recyclerViewChat.adapter = messageAdapter
    }

    private fun setupChatHeader() {
        val chatRef = FirebaseDatabase.getInstance().reference.child("chats").child(chatId)
        chatRef.get().addOnSuccessListener { chatSnap ->
            val groupName = chatSnap.child("groupName").getValue(String::class.java)
            if (!groupName.isNullOrEmpty()){
                //case A: it's a course chat
                tvChatname.text = groupName
            }else {
                //case B: it's a DM or unnamed group
                val fetchedMembers = chatSnap.child("members").children.mapNotNull { it.getValue(String::class.java) }

                //update the class-level variable so sendMessage() works later
                if (fetchedMembers.isNotEmpty()){
                    memberIds = fetchedMembers
                }

                usersRef.get().addOnSuccessListener { snap ->
                    val names = fetchedMembers
                        .filter { it != currentUid }
                        .mapNotNull { uid ->
                            snap.child(uid).child("name").getValue(String::class.java)
                        }

                    if (names.isNotEmpty()){
                        tvChatname.text = names.joinToString(", ")
                    }
                }
            }
        }

    }

    private fun sendMessage(){
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty()) return
        //create message object
        val messageObj = Message(
            senderId = currentUid,
            senderName = currentUserName,
            senderAvatar = currentUserAvatar,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )
        //save the message to the chat room
        dbRef.child(chatId).child("messages").push().setValue(messageObj)

        //determine notification text (group or DM)
        val notifyTitle: String
        val notifyBody: String
        if (isGroup){
            notifyTitle = tvChatname.text.toString()
            notifyBody = "$currentUserName: $messageText"
        }else {
            notifyTitle = currentUserName
            notifyBody = messageText
        }

        //send to all recipients except user
        val recipients = memberIds.filter {it != currentUid}

        for (uid in recipients){
            usersRef.child(uid).child("fcmToken").get().addOnSuccessListener { snapshot ->
                val token = snapshot.value?.toString()
                if (!token.isNullOrEmpty()){
                    sendNotification(token, notifyTitle, notifyBody)
                }
            }
        }

        //clear input box after sending
        etMessage.text.clear()
    }

    //V1 notification sender
    private fun sendNotification(recipientToken: String, title: String, message: String){
        val projectId = "uni-buddy-it2021084"

        val fcmUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        //launch background coroutine to generate token
        CoroutineScope(Dispatchers.IO).launch{
            val accessToken = FcmTokenSender.getAccessToken(applicationContext)

            if (accessToken == null){
                Log.e("FCM", "Failed to generate access token")
                return@launch
            }

            //construct V1 JSON Payload
            val dataPayload = JSONObject()
            dataPayload.put("title", title)
            dataPayload.put("body", message)
            dataPayload.put("chatId", chatId)

            val messagePayload = JSONObject()
            messagePayload.put("token", recipientToken)
            messagePayload.put("data", dataPayload)

            val rootPayload = JSONObject()
            rootPayload.put("message", messagePayload)

            val request = object : JsonObjectRequest(Method.POST, fcmUrl, rootPayload,
                Response.Listener{
                    Log.d("FCM", "Notification sent!")
                },
                Response.ErrorListener{ error ->
                    Log.e("FCM", "Error sending: ${error.networkResponse.statusCode}")
                }) {
                override fun getHeaders(): MutableMap<String, String>{
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = "Bearer $accessToken"
                    headers["Content-Type"] = "application/json"
                    return headers
                }
            }
            //add to volley queue on main thread
            withContext(Dispatchers.Main){
                Volley.newRequestQueue(this@ChatActivity).add(request)
            }
        }
    }
}