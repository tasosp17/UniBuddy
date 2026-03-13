package com.it2021084.unibuddy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private val messageList: List<Message>, private val isGroupchat: Boolean, private var receiverAvatarBase64: String? = null):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM_SENT = 1
        private const val ITEM_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]
        return if (FirebaseAuth.getInstance().currentUser?.uid == currentMessage.senderId){
            ITEM_SENT
        } else {
            ITEM_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        val formattedTime = formatTimestamp(currentMessage.timestamp)

        if (holder is SentViewHolder) {
            holder.sentMessage.text = currentMessage.message
            holder.sentTime.text = formattedTime
        } else if (holder is ReceivedViewHolder) {
            holder.receivedMessage.text = currentMessage.message
            holder.receivedTime.text = formattedTime
            //show groupchat sender name
            if (isGroupchat){
                holder.senderName.visibility = View.VISIBLE
                holder.senderName.text = currentMessage.senderName
            } else {
                holder.senderName.visibility = View.GONE
            }
            //show receiver avatar
            val avatarBase64 = currentMessage.senderAvatar ?: receiverAvatarBase64
            if (avatarBase64 != null) {
                try {
                    val bytes = Base64.decode(avatarBase64, Base64.DEFAULT)
                    holder.avatar.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                } catch (e: Exception){
                    holder.avatar.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                holder.avatar.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    override fun getItemCount(): Int =messageList.size

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val sentMessage: TextView = itemView.findViewById(R.id.tvMessageSent)
        val sentTime: TextView = itemView.findViewById(R.id.tvTimeSent)
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val receivedMessage: TextView = itemView.findViewById(R.id.tvMessageReceived)
        val receivedTime: TextView = itemView.findViewById(R.id.tvTimeReceived)
        val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val senderName: TextView = itemView.findViewById(R.id.tvSenderName)
    }

    fun setReceiverAvatar(base64: String?){
        receiverAvatarBase64 = base64
        notifyDataSetChanged()
    }
}