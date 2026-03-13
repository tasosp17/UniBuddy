package com.it2021084.unibuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CoursesAdapter(
    private var courseList: List<Course>,
    private val selectedIds: HashSet<String>
) : RecyclerView.Adapter<CoursesAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val checkBox: CheckBox = itemView.findViewById(R.id.cbSelect)

        fun bind(course: Course){
            tvName.text = course.name
            //remove listener temporarily to avoid infinite loop when setting state
            checkBox.setOnCheckedChangeListener(null)
            //set current state
            checkBox.isChecked = selectedIds.contains(course.id)

            //add listener back
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked){selectedIds.add(course.id)}
                else{selectedIds.remove(course.id)}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courseList[position])
    }

    override fun getItemCount(): Int = courseList.size

    //update data during search
    fun updateList(newList: List<Course>){
        courseList = newList
        notifyDataSetChanged()
    }
}

