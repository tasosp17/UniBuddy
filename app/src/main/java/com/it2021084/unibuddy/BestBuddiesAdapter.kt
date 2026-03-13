package com.it2021084.unibuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.ReflectionAccessFilter
import java.util.Locale

class BestBuddiesAdapter(
    private var fullList: List<User>,
    private val selectedUsers: Set<String>,
    private val onSelect: (User, Boolean) -> Unit
) : RecyclerView.Adapter<BestBuddiesAdapter.BuddyViewHolder>(), Filterable {

    private var filteredList = ArrayList(fullList)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BuddyViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_select, parent, false)
        return BuddyViewHolder(v)
    }

    override fun onBindViewHolder(holder: BuddyViewHolder, position: Int) {
        val user = filteredList[position]
        holder.username.text = user.name

        //remove listener temporarily to avoid trigger while setting state
        holder.checkbox.setOnCheckedChangeListener(null)

        holder.checkbox.isChecked = selectedUsers.contains(user.uid)

        holder.itemView.setOnClickListener {
            holder.checkbox.isChecked = !holder.checkbox.isChecked
        }

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onSelect(user, isChecked)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    fun updateData(newList: List<User>){
        fullList = newList
        filteredList = ArrayList(newList)
        notifyDataSetChanged()
    }

    inner class BuddyViewHolder(v: View) : RecyclerView.ViewHolder(v){
        val username: TextView = v.findViewById(R.id.tvName)
        val checkbox: CheckBox = v.findViewById(R.id.cbSelect)
    }

    //search filter
    override fun getFilter(): Filter {
        return object : Filter(){
            override fun performFiltering(query: CharSequence?): FilterResults {
                val resultList = if (query.isNullOrEmpty()){
                    fullList
                }else {
                    val q = query.toString().lowercase(Locale.getDefault())
                    fullList.filter { it.name?.lowercase()?.contains(q) == true}
                }
                val res = FilterResults()
                res.values = resultList
                return res
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = ArrayList(results?.values as List<User>)
                notifyDataSetChanged()
            }
        }
    }
}