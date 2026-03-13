package com.it2021084.unibuddy

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ChatsAdapter(
    private val chatList: List<ChatItem>,
    private val onChatClick: (ChatItem) -> Unit
): RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {
    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar = itemView.findViewById<ImageView>(R.id.ivAvatar)
        val tvUsername = itemView.findViewById<TextView>(R.id.tvUsername)
        val tvLastMessage = itemView.findViewById<TextView>(R.id.tvLastMessage)

        fun bind(item: ChatItem){
            tvUsername.text = item.username
            tvLastMessage.text = item.lastMessage

            //visual logic for unread
            if (item.isUnread){
                //bold text for unread
                tvUsername.setTypeface(null, Typeface.BOLD)
                tvLastMessage.setTypeface(null, Typeface.BOLD)
                tvLastMessage.setTextColor(android.graphics.Color.BLACK) //darker message
            }else {
                //normal text for read
                tvUsername.setTypeface(null, Typeface.NORMAL)
                tvLastMessage.setTypeface(null, Typeface.NORMAL)
                tvLastMessage.setTextColor(android.graphics.Color.GRAY)
            }

            //decode avatar
            if(!item.avatar.isNullOrEmpty()){
                val bytes = Base64.decode(item.avatar, Base64.DEFAULT)
                ivAvatar.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
            }
            itemView.setOnClickListener { onChatClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount(): Int = chatList.size

}