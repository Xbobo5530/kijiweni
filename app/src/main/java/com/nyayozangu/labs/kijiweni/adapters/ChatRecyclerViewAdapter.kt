package com.nyayozangu.labs.kijiweni.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.google.firebase.auth.FirebaseAuth
import com.nyayozangu.labs.kijiweni.R
import com.nyayozangu.labs.kijiweni.helpers.Common
import com.nyayozangu.labs.kijiweni.models.ChatMessage

private val common: Common = Common
class ChatRecyclerViewAdapter(private val chatList: List<ChatMessage>,
                              val glide: RequestManager,
                              var context: Context?):
        RecyclerView.Adapter<ChatRecyclerViewAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ChatRecyclerViewAdapter.ViewHolder{
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_list_item, parent, false)
        this.context = parent.context
        return ViewHolder(view)       }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]
        val username = chat.username
        val message = chat.message
        val userImageUrl = chat.user_image_url
        val imageUrl = chat.chat_image_url
        val imagePath = chat.chat_image_path
        val thumbUrl = chat.chat_thumb_url
        val thumbPath = chat.chat_thumb_path
        holder.usernameField.text = username
        holder.messageField.text = message

        handleUserViewVisibility(chat, holder, userImageUrl)
        handleChatImage(imageUrl, thumbUrl, holder)
        handleChatImageByPath(imagePath, thumbPath, holder)

        holder.replyButton.setOnClickListener{handleReplyMessage()}
    }

    private fun handleUserViewVisibility(chat: ChatMessage, holder: ViewHolder, userImageUrl: String?) {
        if (common.uid() == chat.user_id) {
            holder.userImageView.visibility = View.GONE
            holder.currentUserImageView.visibility = View.VISIBLE
            userImageUrl?.let { common.setCircleImage(it, holder.currentUserImageView, glide) }
        } else {
            holder.userImageView.visibility = View.VISIBLE
            holder.currentUserImageView.visibility = View.GONE
            userImageUrl?.let { common.setCircleImage(it, holder.userImageView, glide) }
        }
    }

    private fun handleChatImage(imageUrl: String?, thumbUrl: String?, holder: ViewHolder) {
        if (imageUrl != null && thumbUrl != null){
            holder.chatImageView.visibility = View.VISIBLE
            common.setImage(imageUrl, thumbUrl, holder.chatImageView, glide, holder.progressBar)
        } else {
            holder.chatImageView.visibility = View.GONE
        }
    }

    private fun handleChatImageByPath(imagePath: String?, thumbPath: String?, holder: ChatRecyclerViewAdapter.ViewHolder) {
        if (imagePath != null && thumbPath != null){
            holder.chatImageView.visibility = View.VISIBLE
            common.setImageByPath(imagePath, thumbPath, holder.chatImageView, glide, holder.progressBar)
        } else {
            holder.chatImageView.visibility = View.GONE
        }
    }

    private fun handleReplyMessage() {
        //pass the chat id to the main activity and handle reply from there
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val usernameField: TextView = itemView.findViewById(R.id.usernameTextView)
        val messageField: TextView = itemView.findViewById(R.id.chatMessageTextView)
        val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
        val currentUserImageView: ImageView = itemView.findViewById(R.id.currentUserImageView)
        val replyButton : ImageButton = itemView.findViewById(R.id.replyImageButton)
        val progressBar: ProgressBar = itemView.findViewById(R.id.chatListItemProgressBar)
        val chatImageView: ImageView = itemView.findViewById(R.id.chatImageView)
    }


}