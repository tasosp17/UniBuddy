package com.it2021084.unibuddy

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(private val users: List<User>, private val onItemLongClick: (User, View) -> Unit) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)

        //long click listener for popup menu
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION){
                    onItemLongClick(users[position], it)
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType:Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvUserName.text = user.name

        //show active/inactive status
        val activeIcon = if (user.isActive) R.drawable.status_circle_active else R.drawable.status_circle_inactive
        holder.ivStatus.setImageResource(activeIcon)
        //show avatar
        if(!user.avatar.isNullOrEmpty()){
            val bytes = Base64.decode(user.avatar, Base64.DEFAULT)
            holder.ivAvatar.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
        }

    }

    override fun getItemCount(): Int = users.size
}
