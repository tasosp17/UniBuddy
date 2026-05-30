package com.it2021084.unibuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SocialGroupAdapter(
    private var socialGroupList: List<SocialGroup>,
    private val selectedIds: HashSet<String>
) : RecyclerView.Adapter<SocialGroupAdapter.SocialGroupViewHolder>() {

    inner class SocialGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val checkBox: CheckBox = itemView.findViewById(R.id.cbSelect)

        fun bind(socialGroup: SocialGroup){
            tvName.text = socialGroup.name
            //remove listener temporarily to avoid infinite loop when setting state
            checkBox.setOnCheckedChangeListener(null)
            //set current state
            checkBox.isChecked = selectedIds.contains(socialGroup.id)

            //add listener back
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if(isChecked){selectedIds.add(socialGroup.id)}
                else{selectedIds.remove(socialGroup.id)}
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocialGroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select, parent, false)
        return SocialGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: SocialGroupViewHolder, position: Int) {
        holder.bind(socialGroupList[position])
    }

    override fun getItemCount(): Int = socialGroupList.size

    //update data during search
    fun updateList(newList: List<SocialGroup>){
        socialGroupList = newList
        notifyDataSetChanged()
    }
}