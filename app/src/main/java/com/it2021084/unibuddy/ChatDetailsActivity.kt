package com.it2021084.unibuddy

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class ChatDetailsActivity: AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvChatname: TextView
    private lateinit var ivChatAvatar: ImageView
    private lateinit var llEditActions: LinearLayout
    private lateinit var btnEditName: Button
    private lateinit var btnEditImage: Button
    private lateinit var rvMembers: RecyclerView

    private lateinit var dbRef: DatabaseReference
    private var memberList = mutableListOf<User>()
    private lateinit var adapter: MembersAdapter

    private var currentChatId: String? = null
    private var isGroup: Boolean = false

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null && currentChatId != null) {
                try {
                    //convert uri to bitmap
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    }
                    //compress and upload
                    uploadGroupAvatar(bitmap)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_details)

        dbRef = FirebaseDatabase.getInstance().reference //database reference
        //set UI elements
        btnBack = findViewById(R.id.btnBack)
        tvChatname = findViewById(R.id.tvChatName)
        ivChatAvatar = findViewById(R.id.ivChatAvatar)
        llEditActions = findViewById(R.id.llEditActions)
        btnEditName = findViewById(R.id.btnEditName)
        btnEditImage = findViewById(R.id.btnEditImage)
        rvMembers = findViewById(R.id.rvMembers)

        //get intent data
        currentChatId = intent.getStringExtra("chatId")
        isGroup = intent.getBooleanExtra("isGroup", false)

        setupRecyclerView()
        loadChatDetails()

        //LISTENERS
        btnBack.setOnClickListener { finish() }

        btnEditName.setOnClickListener { showEditNameDialog() }

        btnEditImage.setOnClickListener { pickImageLauncher.launch("image/*") }
    }

    private fun setupRecyclerView() {
        val currentUserId = FirebaseAuth.getInstance().uid!! //get ID

        //pass ID to adapter constructor
        adapter = MembersAdapter(memberList, currentUserId){clickedUserId ->
            if (currentUserId == clickedUserId){return@MembersAdapter}

            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("userId", clickedUserId)
            startActivity(intent)
        }
        rvMembers.layoutManager = LinearLayoutManager(this)
        rvMembers.adapter = adapter
    }

    private fun loadChatDetails() {
        val chatId = currentChatId ?: return
        val currentUserId = FirebaseAuth.getInstance().uid!! //get current ID

        dbRef.child("chats").child(chatId).get().addOnSuccessListener { snapshot ->
            //setup header info
            val groupName = snapshot.child("groupName").value?.toString()
            val groupAvatar = snapshot.child("groupAvatar").value?.toString()

            //set details
            if (!groupName.isNullOrEmpty()) {
                //case A: course or named group
                tvChatname.text = groupName
                //set avatar
                if (!groupAvatar.isNullOrEmpty()) {
                    try {
                        val bytes = Base64.decode(groupAvatar, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ivChatAvatar.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        ivChatAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
            } else {
                //case B: it's a DM -> fetch other user's info
                val memberIds = snapshot.child("members").children.mapNotNull { it.getValue(String::class.java) }
                //find the ID that isn't user
                val otherId = memberIds.firstOrNull{it != currentUserId}

                if(otherId != null){
                    //fetch that user's name and avatar
                    dbRef.child("users").child(otherId).get().addOnSuccessListener { userSnap ->
                        val name = userSnap.child("name").value?.toString() ?: "User"
                        val avatar = userSnap.child("avatar").value?.toString()

                        tvChatname.text = name

                        //set avatar
                        if (!avatar.isNullOrEmpty()) {
                            try {
                                val bytes = Base64.decode(avatar, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                ivChatAvatar.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                ivChatAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                            }
                        } else {
                            ivChatAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                }else {
                    tvChatname.text = "Chat Details" //fallback if something goes wrong
                }
            }

            //show/hide edit buttons
            if (isGroup) {
                llEditActions.visibility = View.VISIBLE
            } else {
                llEditActions.visibility = View.GONE
            }

            //load members
            val memberIds =
                snapshot.child("members").children.mapNotNull { it.getValue(String::class.java) }
            loadMembersList(memberIds)
        }
    }

    private fun loadMembersList(ids: List<String>) {
        memberList.clear()
        var loadedCount = 0

        for (uid in ids) {
            dbRef.child("users").child(uid).get().addOnSuccessListener { userSnap ->
                val user = userSnap.getValue(User::class.java)
                if (user != null) {
                    user.uid = uid
                    memberList.add(user)
                }
                loadedCount++
                if (loadedCount == ids.size) {
                    //all loaded, refresh list
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun showEditNameDialog() {
        val editText = EditText(this)
        editText.hint = "Enter new group name"
        //pre-fill with current name
        editText.setText(tvChatname.text)

        AlertDialog.Builder(this)
            .setTitle("Change Group Name")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && currentChatId != null) {
                    updateGroupName(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //update name in firebase
    private fun updateGroupName(newName: String){
        dbRef.child("chats").child(currentChatId!!).child("groupName").setValue(newName)
            .addOnSuccessListener {
                tvChatname.text = newName // update UI immediately
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show()            }
    }

    //compress bitmap >> base64 >> firebase
    private fun uploadGroupAvatar(bitmap: Bitmap) {
        //resize image to prevent lag/crashing
        val scaledBitmap = scaleBitmap(bitmap)

        //compress to byte array
        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream) //70% quality
        val bytes = stream.toByteArray()

        //encode to base64 string
        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)

        //save to firebase
        dbRef.child("chats").child(currentChatId!!).child("groupAvatar").setValue(base64String)
            .addOnSuccessListener {
                ivChatAvatar.setImageBitmap(scaledBitmap) //update UI immediately
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update avatar", Toast.LENGTH_SHORT).show()
            }

    }

    //helper to resize larger photos
    private fun scaleBitmap(bitmap: Bitmap): Bitmap{
        val maxWidth = 600
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth) return bitmap // no need to resize

        val ratio = width.toFloat() / height.toFloat()
        val newHeight = (maxWidth/ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }
}