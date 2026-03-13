package com.it2021084.unibuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SharedCoursesAdapter(
    private val courseList: List<Course>,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<SharedCoursesAdapter.ViewHolder>(){

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val tvName: TextView = itemView.findViewById(R.id.tvCourseName)
        fun bind(course: Course){
            tvName.text = course.name

            itemView.setOnClickListener { onCourseClick(course) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_course, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(courseList[position])
    }

    override fun getItemCount(): Int = courseList.size
}