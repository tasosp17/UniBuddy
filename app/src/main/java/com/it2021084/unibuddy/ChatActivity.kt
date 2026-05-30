package com.it2021084.unibuddy

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class ChatActivity: AppCompatActivity() {

    //UI elements
    private lateinit var btnBack: ImageButton
    private lateinit var tvChatname: TextView
    private lateinit var recyclerViewChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnAttachImage: ImageButton
    private lateinit var btnAttachFile: ImageButton

    // request codes for media pickers
    private val PICK_IMAGE_REQ = 201
    private val PICK_FILE_REQ = 202

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
    private val liveAvatarsMap = mutableMapOf<String, String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // initialize Cloudinary SDK defensively
        initCloudinary()

        //initialize views
        btnBack = findViewById(R.id.btnBack)
        tvChatname = findViewById(R.id.tvChatName)
        recyclerViewChat = findViewById(R.id.recyclerViewChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnAttachImage = findViewById(R.id.btnAttachImage)
        btnAttachFile = findViewById(R.id.btnAttachFile)

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

        //click listeners for attachments
        btnAttachImage.setOnClickListener { openImagePicker() }
        btnAttachFile.setOnClickListener { openFilePicker() }

        //send button functionality
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            sendMessage(messageText, null, "text")
        }

        //back button functionality
        btnBack.setOnClickListener { handleBackNavigation() }

        //handle android system back button
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true){
            override fun handleOnBackPressed(){
                handleBackNavigation()
            }
        })

        //open Chat Details when tapping header
        tvChatname.setOnClickListener {
            val intent = Intent(this, ChatDetailsActivity::class.java)
            //pass the crucial info to the details screen
            intent.putExtra("chatId", chatId)
            intent.putExtra("isGroup", isGroup)
            startActivity(intent)
        }
    }

    //initialize Cloudinary
    private fun initCloudinary(){
        try{
            val config = mapOf(
                "cloud_name" to "dveoahsyc",
                "secure" to true
            )
            MediaManager.init(this, config)
        }catch (e: IllegalStateException){}
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
                        //inject the live avatar, if we have it
                        if (liveAvatarsMap.containsKey(message.senderId)){
                            message.senderAvatar = liveAvatarsMap[message.senderId]
                        }
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
                    val names = memberIds
                        .filter { it != currentUid }
                        .mapNotNull { uid ->
                            snap.child(uid).child("name").getValue(String::class.java)
                        }

                    //cache the fresh avatars for everyone
                    for (uid in memberIds){
                        val freshAvatar = snap.child(uid).child("avatar").getValue(String::class.java)
                        if (freshAvatar != null){
                            liveAvatarsMap[uid] = freshAvatar
                        }
                    }

                    //if messages loaded before the avatars finished downloading, update them now
                    if (messageList.isNotEmpty()){
                        for (msg in messageList){
                            if (liveAvatarsMap.containsKey(msg.senderId)){
                                msg.senderAvatar = liveAvatarsMap[msg.senderId] //overwrite old hardcoded avatar
                            }
                        }
                        messageAdapter.notifyDataSetChanged()
                    }

                    if (names.isNotEmpty()){
                        tvChatname.text = names.joinToString(", ")
                    }
                }
            }
        }

    }

    private fun sendMessage(messageText: String, fileUrl: String?, fileType: String?){
         if (messageText.isEmpty() && fileUrl == null) return
        val timestamp = System.currentTimeMillis()
        //create message object
        val messageObj = Message(
            senderId = currentUid,
            senderName = currentUserName,
            senderAvatar = currentUserAvatar,
            message = messageText,
            timestamp = timestamp,
            fileUrl = fileUrl,
            fileType = fileType
        )
        //save the message to the chat room
        dbRef.child(chatId).child("messages").push().setValue(messageObj)

        //ensure the chat has a members list and type, so ChatsActivity can find it
        dbRef.child(chatId).child("members").setValue(memberIds)
        dbRef.child(chatId).child("isGroup").setValue(isGroup)

        //determine notification text (group or DM)
        val notifyTitle: String = if (isGroup) tvChatname.text.toString() else currentUserName
        val notifyBody: String = if (isGroup && fileUrl == null) "$currentUserName: $messageText" else messageText

        //send to all recipients except user
        val recipients = memberIds.filter {it != currentUid}

        for (uid in recipients){
            val recipientHistoryRef = FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users").child(uid).child("notificationHistory").push()

            val chatHistoryNotif = AppNotification(
                id = recipientHistoryRef.key ?: "",
                title = notifyTitle,
                body = notifyBody,
                type = "chat",
                timestamp = timestamp,
                payloadId = chatId,  // Key target to open this exact conversation room
                senderName = currentUserName,
                senderAvatar = currentUserAvatar ?: ""
            )
            recipientHistoryRef.setValue(chatHistoryNotif)

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

        CoroutineScope(Dispatchers.IO).launch{
            val accessToken = FcmTokenSender.getAccessToken(applicationContext) ?: return@launch

            val dataPayload = JSONObject().apply {
                put("type", "chat")
                put("title", title)
                put("body", message)
                put("chatId", chatId)
                put("senderName", currentUserName)
                put("senderAvatar", currentUserAvatar ?: "")
            }

            val messagePayload = JSONObject().apply {
                put("token", recipientToken)
                put("data", dataPayload)
            }

            val rootPayload = JSONObject().apply { put("message", messagePayload) }

            val request = object : JsonObjectRequest(Method.POST, fcmUrl, rootPayload,
                Response.Listener { Log.d("FCM", "Notification sent successfully!") },
                Response.ErrorListener { error -> Log.e("FCM", "FCM Volley Error: ${error.networkResponse?.statusCode}") }) {
                override fun getHeaders(): MutableMap<String, String>{
                    return hashMapOf(
                        "Authorization" to "Bearer $accessToken",
                        "Content-Type" to "application/json"
                    )
                }
            }
            // FIXED: Using single applicationContext queue explicitly drops leakage locks
            withContext(Dispatchers.Main){
                Volley.newRequestQueue(applicationContext).add(request)
            }
        }
    }

    private fun handleBackNavigation(){
        if (isTaskRoot) {
            //app was opened from a notification, route user to ChatsActivity
            val intent = Intent(this, ChatsActivity::class.java)
            startActivity(intent)
            finish()
        }else {
            //normal flow: just go back
            finish()
        }
    }

    private fun openImagePicker(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type= "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQ)
    }

    private fun openFilePicker(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply{
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Select PDF file"), PICK_FILE_REQ)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null){
            val selectedMediaUri = data.data!!
            when (requestCode){
                PICK_IMAGE_REQ -> uploadToCloudinary(selectedMediaUri, "image")
                PICK_FILE_REQ -> uploadToCloudinary(selectedMediaUri, "pdf")
            }
        }
    }

    private fun uploadToCloudinary(uri: Uri, type: String){
        Toast.makeText(this, "Uploading attachment...", Toast.LENGTH_SHORT).show()

        MediaManager.get().upload(uri)
            .option("resource_type", "auto")
            .unsigned("UniBuddy")
            .callback(object: UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long){}

                override fun onSuccess(requestId: String, resultData: Map<*, *>){
                    val secureUrl = resultData["secure_url"] as? String ?: ""
                    if (secureUrl.isNotEmpty()){
                        // push metadata to firebase database upon upload access
                        val displayBody = if (type == "image") "📷 Sent a photo" else "📄 Sent a PDF Document"
                        sendMessage(displayBody, secureUrl, type)
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo){
                    Log.e("CLOUDINARY_UPLOAD", "Uplaod Failed: ${error.description}")
                    Toast.makeText(this@ChatActivity, "Failed to upload file", Toast.LENGTH_SHORT).show()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo){}
            }).dispatch()
    }

}