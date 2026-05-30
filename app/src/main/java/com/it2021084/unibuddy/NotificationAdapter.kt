package com.it2021084.unibuddy

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val items: List<AppNotification>,
    private val onItemClick: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val title = v.findViewById<TextView>(R.id.tvLogTitle)
        val body = v.findViewById<TextView>(R.id.tvLogBody)
        val time = v.findViewById<TextView>(R.id.tvLogTime)
        val avatar = v.findViewById<ImageView>(R.id.ivNotifAvatar)

        fun bind(item: AppNotification) {
            val context = itemView.context

            // Format Display Headings Natively
            title.text = if (item.senderName.isNotEmpty()) item.senderName else item.title
            body.text = item.body

            val sdf = SimpleDateFormat("HH:mm, MMM dd", Locale.getDefault())
            time.text = sdf.format(Date(item.timestamp))

            // Adapt Avatar Rendering Layers Dynamically
            if (item.senderAvatar == "ADMIN_ICON") {
                avatar.setImageResource(R.drawable.secretary) // Render system badge
            } else if (item.senderAvatar == "LECTURE_ICON"){
                avatar.setImageResource(R.drawable.ic_upcoming_lectures)
            } else if (item.senderAvatar.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(item.senderAvatar, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    avatar.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    fallbackAvatar()
                }
            } else {
                fallbackAvatar()
            }

            itemView.setOnClickListener { onItemClick(item) }
        }

        private fun fallbackAvatar() {
            Glide.with(itemView.context)
                .load(R.drawable.ic_profile_placeholder)
                .apply(RequestOptions.circleCropTransform())
                .into(avatar)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
}