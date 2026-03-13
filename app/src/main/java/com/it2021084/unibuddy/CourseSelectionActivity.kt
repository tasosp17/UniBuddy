package com.it2021084.unibuddy

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CourseSelectionActivity: AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnSave: Button
    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var adapter: CoursesAdapter

    //tracks which course IDs are currently checked
    private val selectedCourseIds = HashSet<String>()

    private val currentUserId = FirebaseAuth.getInstance().uid!!
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_selection)

        dbRef = FirebaseDatabase.getInstance().reference

        recyclerView = findViewById(R.id.rvCourses)
        btnSave = findViewById(R.id.btnSaveCourses)
        btnBack = findViewById(R.id.btnBack)
        etSearch = findViewById(R.id.etSearch)

        setupRecyclerView()
        setupSearchListener()
        loadCurrentSelection()

        btnSave.setOnClickListener { saveChanges() }

        //back button functionality
        btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView(){
        //pass the static catalog and the selection set to the adapter
        adapter = CoursesAdapter(CourseCatalog.allCourses, selectedCourseIds)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    //search logic
    private fun setupSearchListener(){
        etSearch.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                filterCourses(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterCourses(query: String){
        if (query.isEmpty()){
            //no search text, show all courses
            adapter.updateList(CourseCatalog.allCourses)
        } else {
            //filter list by name
            val filteredList = CourseCatalog.allCourses.filter {
                it.name.contains(query, ignoreCase = true)
            }
            adapter.updateList(filteredList)
        }
    }
    private fun loadCurrentSelection(){
        //get user's previously saved courses from firebase
        dbRef.child("users").child(currentUserId).child("enrolledCourses").get()
            .addOnSuccessListener { snapshot ->
                selectedCourseIds.clear()
                for(child in snapshot.children){
                    child.key?.let{selectedCourseIds.add(it)}
                }
                adapter.notifyDataSetChanged()

            }
    }

    private fun saveChanges(){
        btnSave.isEnabled = false
        btnSave.text = "Saving..."

        //prepare data for user node
        val userMapUpdate = HashMap<String, Any>()
        selectedCourseIds.forEach { id -> userMapUpdate[id] = true }

        //overwrite the enrolledCourses node in firebase
        dbRef.child("users").child(currentUserId).child("enrolledCourses")
            .setValue(userMapUpdate)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    //if updated, update the chatrooms
                    updateGroupChatMembers()
                    //and update alarms
                    CourseAlarmScheduler.scheduleAlarmsForCourses(this, selectedCourseIds)
                }else{
                    Toast.makeText(this, "Error saving changes", Toast.LENGTH_SHORT).show()
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                }
            }
    }

    //loop through every course in the catalog to check if we should Join or Leave
    private fun updateGroupChatMembers(){
        var pendingUpdates = CourseCatalog.allCourses.size

        if (pendingUpdates == 0){
            finish()
            return
        }

        for (course in CourseCatalog.allCourses){
            val chatRef = dbRef.child("chats").child(course.id)

            //case A: user selected this course, add them to chat
            if (selectedCourseIds.contains(course.id)){
                //create groupchat if it doesn't exist
                val chatInfo = mapOf(
                    "isGroup" to true,
                    "groupName" to course.name
                )
                chatRef.updateChildren(chatInfo)

                //add user ID to members list
                chatRef.child("members").get().addOnSuccessListener { snap ->
                    val members = snap.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                    if (!members.contains(currentUserId)){
                        members.add(currentUserId)
                        chatRef.child("members").setValue(members)
                    }
                    checkIfDone(--pendingUpdates)
                }
            }
            //case B: user hasn't selected course, remove from groupchat
            else{
                chatRef.child("members").get().addOnSuccessListener { snap ->
                    val members = snap.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                    if(members.contains(currentUserId)){
                        members.remove(currentUserId)
                        chatRef.child("members").setValue(members)
                    }
                    checkIfDone(--pendingUpdates)
                }
            }
        }

    }

    //simple helper to close screen when all firebase updates are done
    private fun checkIfDone(remaining: Int){
        if(remaining <= 0){
            finish() //close activity and go back
        }
    }
}