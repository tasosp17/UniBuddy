package com.it2021084.unibuddy

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MembersAdapter(
    private val memberList: List<User>,
    private val currentUserId: String,
    private val onMemberClick: (String) -> Unit
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)

        fun bind(user: User) {
            tvName.text = user.name ?: "Unknown"

            //status icon logic (hide user's)
            if (user.uid == currentUserId){
                ivStatus.visibility = View.GONE
            }else{
                ivStatus.visibility  = View.VISIBLE

                if (user.isActive){
                    ivStatus.setImageResource(R.drawable.status_circle_active)
                }else{
                    ivStatus.setImageResource(R.drawable.status_circle_inactive)
                }
            }

            //avatar logic
            if (!user.avatar.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(user.avatar, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ivAvatar.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
            }
            itemView.setOnClickListener { onMemberClick(user.uid) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(memberList[position])
    }

    override fun getItemCount(): Int = memberList.size
}