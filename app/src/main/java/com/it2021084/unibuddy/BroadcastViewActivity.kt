package com.it2021084.unibuddy

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BroadcastViewActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast_view)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val tvBroadcastBody = findViewById<TextView>(R.id.tvBroadcastBody)

        val messageContent = intent.getStringExtra("broadcast_body") ?: "No message content provided."
        tvBroadcastBody.text = messageContent

        btnBack.setOnClickListener { finish() }
    }
}