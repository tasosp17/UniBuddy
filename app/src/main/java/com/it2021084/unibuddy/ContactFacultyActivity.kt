package com.it2021084.unibuddy

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContactFacultyActivity: AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var etSubject: EditText
    private lateinit var etBody: EditText
    private lateinit var btnSend: Button
    private lateinit var btnCancel: Button

    //get current logged in user's email
    private val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_faculty)

        spinner = findViewById(R.id.spinnerFaculty)
        etSubject = findViewById(R.id.etSubject)
        etBody = findViewById(R.id.etBody)
        btnSend = findViewById(R.id.btnSendEmail)
        btnCancel = findViewById(R.id.btnCancel)

        setupSpinner()

        btnSend.setOnClickListener { sendEmailInBackground() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun setupSpinner(){
        val facultyNames = FacultyCatalog.facultyList.map { "${it.name} (${it.department})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, facultyNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun sendEmailInBackground(){
        val selectedPosition = spinner.selectedItemPosition
        val selectedFaculty = FacultyCatalog.facultyList[selectedPosition]

        val subject = etSubject.text.toString().trim()
        val bodyInput = etBody.text.toString().trim()

        if (subject.isEmpty() || bodyInput.isEmpty()){
            Toast.makeText(this, "Please fill in all fields" , Toast.LENGTH_SHORT).show()
            return
        }

        //disable button to prevent double clicks
        btnSend.isEnabled = false
        btnSend.text = "Sending..."

        //construct a professional footer
        val finalBody = """
            $bodyInput
            ______________________________
            Sent via UniBuddy App
            From: $currentUserEmail
        """.trimIndent()

        //launch background task
        CoroutineScope(Dispatchers.Main).launch{
            val success = EmailService.sendEmail(
                recipientEmail = selectedFaculty.email,
                subject = "[UniBuddy] $subject" ,
                body = finalBody,
                replyToEmail = currentUserEmail //replies go to student
            )

            if (success){
                Toast.makeText(applicationContext, "Email sent successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }else {
                Toast.makeText(applicationContext, "Failed to send.", Toast.LENGTH_SHORT).show()
                btnSend.isEnabled = true
                btnSend.text = "Send Message"
            }
        }
    }
}