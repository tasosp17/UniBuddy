package com.it2021084.unibuddy

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*



class NewChatFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var rvUsers: RecyclerView
    private lateinit var btnStartChat: Button
    private lateinit var btnBack: ImageButton

    private val userList = mutableListOf<User>()
    private val selectedUsers = mutableSetOf<User>()
    private lateinit var adapter: NewChatAdapter

    private lateinit var dbRef: DatabaseReference
    private lateinit var currentUid: String
    private lateinit var btnContactFaculty: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_new_chat, container, false)

        etSearch = view.findViewById(R.id.etSearchUser)
        rvUsers = view.findViewById(R.id.rvUsers)
        btnStartChat = view.findViewById(R.id.btnStartChat)
        btnBack = view.findViewById(R.id.btnBack)
        btnContactFaculty = view.findViewById(R.id.btnContactFaculty)

        currentUid = FirebaseAuth.getInstance().uid!!
        dbRef = FirebaseDatabase.getInstance().getReference("users")

        adapter = NewChatAdapter(userList, selectedUsers) { user, isSelected ->
            if (isSelected) selectedUsers.add(user) else selectedUsers.remove(user)
        }

        rvUsers.layoutManager = LinearLayoutManager(requireContext())
        rvUsers.adapter = adapter

        loadUsers()

        //search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter?.filter(s.toString())
            }
        })

        btnStartChat.setOnClickListener {
            if (selectedUsers.isNotEmpty()) {
                createOrOpenChat()
            }
        }

        btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            // restore BottomNavigationView
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNav.visibility = View.VISIBLE
            bottomNav.isEnabled = true

            //restore new chat button
            val fab = requireActivity().findViewById<View>(R.id.fabNewMessage)
            fab.visibility = View.VISIBLE

            // hide fragment container if needed
            val container = requireActivity().findViewById<FrameLayout>(R.id.fragmentContainer)
            container.visibility = View.GONE

        }

        btnContactFaculty.setOnClickListener {
            val intent = Intent(requireContext(), ContactFacultyActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun loadUsers() {
        //fetch the current user's SSID
        dbRef.child(currentUid).child("ssid").get().addOnSuccessListener { ssidSnapshot ->
            val mySsid = ssidSnapshot.value?.toString() ?: ""


            //fetch list of all users
            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userList.clear()

                    for (child in snapshot.children) {
                        val user = child.getValue(User::class.java) ?: continue
                        if (user.uid != currentUid && user.ssid == mySsid) {
                            userList.add(user)
                        }
                    }
                    adapter.updateData(userList)
                }

                override fun onCancelled(p0: DatabaseError) {}
            })
        }
    }

    private fun createOrOpenChat() {
        val memberIds = (selectedUsers.map { it.uid } + currentUid).distinct()
        val chatId = memberIds.sorted().joinToString("_")

        //save chat info in firebase
        val chatInfo = mutableMapOf<String, Any>(
            "members" to memberIds, //store all member UIDs
            "isGroup" to (selectedUsers.size > 1)
        )

        FirebaseDatabase.getInstance().getReference("chats").child(chatId).updateChildren(chatInfo)

        val intent = Intent(requireContext(), ChatActivity::class.java).apply() {
            putExtra("chatId", chatId)
            putExtra("isGroup", selectedUsers.size > 1)
            putStringArrayListExtra("memberUids", ArrayList(memberIds))
            if (selectedUsers.size == 1) {
                val only = selectedUsers.first()
                putExtra("receiverUid", only.uid)
                putExtra("receiverName", only.name)
            }
        }
        startActivity(intent)
    }

}