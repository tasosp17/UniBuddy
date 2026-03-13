package com.it2021084.unibuddy

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BestBuddiesActivity: AppCompatActivity() {

    private lateinit var rvBuddies: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton

    private lateinit var adapter: BestBuddiesAdapter
    private val userList = mutableListOf<User>()

    private val selectedBuddiesIds = mutableSetOf<String>()

    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_best_buddies)

        currentUid = FirebaseAuth.getInstance().uid!!
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        rvBuddies = findViewById(R.id.rvBestBuddies)
        etSearch = findViewById(R.id.etSearch)
        btnSave  = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        //initialize adapter
        adapter = BestBuddiesAdapter(userList, selectedBuddiesIds){user, isSelected ->
            if (isSelected){
                selectedBuddiesIds.add(user.uid)
            }else {
                selectedBuddiesIds.remove(user.uid)
            }
        }

        rvBuddies.layoutManager = LinearLayoutManager(this)
        rvBuddies.adapter = adapter

        //load data
        loadData()

        //search listener
        etSearch.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s.toString())
            }
        })

        //save listener
        btnSave.setOnClickListener {
            saveBestBuddies()
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun loadData(){
        //get user SSID
        dbRef.child(currentUid).child("ssid").get().addOnSuccessListener { ssidSnap ->
            val mySsid = ssidSnap.value?.toString() ?: ""

            //get user existing best buddies
            dbRef.child(currentUid).child("bestBuddies").get().addOnSuccessListener { buddiesSnap ->
                selectedBuddiesIds.clear()
                for (child in buddiesSnap.children){
                    child.key?.let {selectedBuddiesIds.add(it)}
                }

                //get all users and filter
                dbRef.addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        userList.clear()
                        for (child in snapshot.children){
                            val user = child.getValue(User::class.java) ?: continue

                            //filter: not ME and same ssid
                            if (user.uid != currentUid && user.ssid == mySsid){
                                userList.add(user)
                            }
                        }
                        //sort selected buddies to be on top
                        userList.sortWith(compareByDescending<User> {
                            selectedBuddiesIds.contains(it.uid)
                        }.thenBy { it.name })

                        adapter.updateData(userList)
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun saveBestBuddies(){
        val updates = mutableMapOf<String, Any?>()

        //replace entire bestBuddies node for this user
        val buddiesMap = mutableMapOf<String, Boolean>()
        for (uid in selectedBuddiesIds){
            buddiesMap[uid] = true
        }
        dbRef.child(currentUid).child("bestBuddies").setValue(buddiesMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Best Buddies list updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save.", Toast.LENGTH_SHORT).show()
            }
    }
}