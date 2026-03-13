package com.it2021084.unibuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UpcomingLectureItem(
    val courseName: String,
    val timestamp: Long
)

class UpcomingLecturesAdapter (private val lectures: List<UpcomingLectureItem>):
RecyclerView.Adapter<UpcomingLecturesAdapter.LectureViewHolder>(){

    inner class LectureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvName: TextView = itemView.findViewById(R.id.tvLectureName)
        val tvTime: TextView = itemView.findViewById(R.id.tvLectureTime)

        fun bind(item: UpcomingLectureItem){
            tvName.text = item.courseName
            //format timestamp to readable string
            val sdf = SimpleDateFormat("EEEE, HH:mm", Locale.getDefault())
            tvTime.text = sdf.format(Date(item.timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LectureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lecture, parent, false)
        return LectureViewHolder(view)
    }

    override fun onBindViewHolder(holder: LectureViewHolder, position: Int) {
        holder.bind(lectures[position])
    }

    override fun getItemCount(): Int = lectures.size
}