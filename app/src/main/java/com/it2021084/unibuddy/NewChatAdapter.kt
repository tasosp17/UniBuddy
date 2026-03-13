package com.it2021084.unibuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.it2021084.unibuddy.R
import com.it2021084.unibuddy.User
import java.util.*
import kotlin.collections.ArrayList

class NewChatAdapter(
    private var fullList: List<User>,
    private val selectedUsers: Set<User>,
    private val onSelect: (User, Boolean) -> Unit
): RecyclerView.Adapter<NewChatAdapter.UserViewHolder>(), Filterable {

    private var filteredList = ArrayList(fullList)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_select, parent, false)
        return UserViewHolder(v)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = filteredList[position]
        holder.username.text = user.name
        holder.checkbox.isChecked = selectedUsers.contains(user)

        holder.itemView.setOnClickListener{
            holder.checkbox.isChecked = !holder.checkbox.isChecked
        }
        holder.checkbox.setOnCheckedChangeListener{_, isChecked ->
            onSelect(user, isChecked)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    fun updateData(newList: List<User>){
        fullList = newList
        filteredList = ArrayList(newList)
        notifyDataSetChanged()
    }

    inner class UserViewHolder(v: View): RecyclerView.ViewHolder(v){
        val username: TextView = v.findViewById(R.id.tvName)
        val checkbox: CheckBox = v.findViewById(R.id.cbSelect)
    }

    //search filtering
    override fun getFilter(): Filter? {
        return object: Filter(){
            override fun performFiltering(query: CharSequence?): FilterResults {
                val resultList = if (query.isNullOrEmpty()){
                    fullList
                }else {
                    val q = query.toString().lowercase(Locale.getDefault())
                    fullList.filter{it.name?.lowercase()?.contains(q)==true}
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