package com.it2021084.unibuddy

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SecretaryBroadcastActivity: AppCompatActivity() {

    private lateinit var etMessage: EditText
    private lateinit var btnPublish: Button
    private lateinit var btnSignOut: Button
    private val DB_URL = "https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secretary_broadcast)

        etMessage = findViewById(R.id.etBroadcastMessage)
        btnPublish = findViewById(R.id.btnPublishBroadcast)
        btnSignOut = findViewById(R.id.btnSecretarySignOut)

        btnPublish.setOnClickListener {
            val broadcastText = etMessage.text.toString().trim()
            if(broadcastText.isEmpty()){
                Toast.makeText(this, "Message content cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnPublish.isEnabled = false
            btnPublish.text = "Sending..."
            dispatchGlobalBroadcast(broadcastText)
        }

        btnSignOut.setOnClickListener { logoutUser() }
    }

    private fun dispatchGlobalBroadcast(messageText: String){
        val database = FirebaseDatabase.getInstance(DB_URL)
        val usersRef = database.getReference("users")

        usersRef.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRecipients = 0
                val timestamp = System.currentTimeMillis()

                for (userSnapshot in snapshot.children){
                    val targetUid = userSnapshot.key ?: continue

                    // 1. Direct Database Infiltration Push Strategy
                    val userNotifRef = usersRef.child(targetUid).child("notificationHistory").push()
                    val appNotif = AppNotification(
                        id = userNotifRef.key ?: "",
                        title = "Campus Administration",
                        body = messageText,
                        type = "broadcast",
                        timestamp = timestamp,
                        payloadId = messageText,
                        senderName = "University Secretariat",
                        senderAvatar = "ADMIN_ICON"
                    )
                    userNotifRef.setValue(appNotif)

                    // 2. Dispatch hardware push popups
                    val token = userSnapshot.child("fcmToken").value?.toString()
                    if(!token.isNullOrEmpty()){
                        sendBroadcastFcmMessage(token, messageText)
                    }
                    totalRecipients++
                }

                Toast.makeText(this@SecretaryBroadcastActivity, "Broadcast synchronized across $totalRecipients profiles", Toast.LENGTH_SHORT).show()
                etMessage.text.clear()
                btnPublish.isEnabled = true
                btnPublish.text = "Publish Announcement"
            }

            override fun onCancelled(error: DatabaseError) {
                btnPublish.isEnabled = true
                btnPublish.text = "Publish Announcement"
            }
        })
    }

    private fun sendBroadcastFcmMessage(recipientToken: String, announcement: String){
        val projectId = "uni-buddy-it2021084"
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

        CoroutineScope(Dispatchers.IO).launch{
            val accessToken = FcmTokenSender.getAccessToken(applicationContext) ?: return@launch
            val dataPayload = JSONObject().apply{
                put("type", "broadcast")
                put("title", "Campus Administration")
                put("body", announcement)
                put("chatId", "broadcast_global")
                put("senderName", "University Secretariat")
                put("senderAvatar", "ADMIN_ICON")
            }

            val messagePayload = JSONObject().apply{
                put("token", recipientToken)
                put("data", dataPayload)
            }

            val rootPayload = JSONObject().apply{ put("message", messagePayload) }

            val request = object: JsonObjectRequest(Method.POST, fcmUrl, rootPayload,
                Response.Listener { Log.d("SECRETARY_FCM", "Broadcast payload delivered successfully")},
                Response.ErrorListener { error -> Log.e("SECRETARY_FCM", "HTTP Error: ${error.networkResponse?.statusCode}")}
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return hashMapOf(
                        "Authorization" to "Bearer $accessToken",
                        "Content-Type" to "application/json"
                    )
                }
            }
            withContext(Dispatchers.Main){
                Volley.newRequestQueue(applicationContext).add(request)
            }
        }
    }

    private fun logoutUser(){
        FirebaseAuth.getInstance().signOut()
        val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
        googleSignInClient.signOut().addOnCompleteListener{
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}