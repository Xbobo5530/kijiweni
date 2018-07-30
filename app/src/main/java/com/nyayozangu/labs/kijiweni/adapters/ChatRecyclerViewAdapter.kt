package com.nyayozangu.labs.kijiweni.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.nyayozangu.labs.kijiweni.R
import com.nyayozangu.labs.kijiweni.helpers.*
import com.nyayozangu.labs.kijiweni.models.ChatMessage

private val common: Common = Common
class ChatRecyclerViewAdapter(private val chatList: List<ChatMessage>,
                              private val glide: RequestManager,
                              private var context: Context?,
                              private val callback: AdapterCallback?):
        RecyclerView.Adapter<ChatRecyclerViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            ChatRecyclerViewAdapter.ViewHolder{
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_list_item, parent, false)
        this.context = parent.context
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]
        val username = chat.username
        val message = chat.message
        holder.setUserData(username)
        holder.setMessage(message)
        holder.handleUserViewVisibility(chat, glide)
        holder.handleChatImage(chat, glide, context)
        holder.replyButton.setOnClickListener{ handleReplyMessage(callback, chat.chat_id) }
    }


    private fun handleReplyMessage(callback: AdapterCallback?, chatId: String?) {
        //pass the chat id to the main activity and handle reply from there
        callback?.reply(chatId)
        Log.d(TAG, "reply button clicked in adapter")
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val usernameField: TextView = itemView.findViewById(R.id.usernameTextView)
        private val messageField: TextView = itemView.findViewById(R.id.chatMessageTextView)
        val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
        val currentUserImageView: ImageView = itemView.findViewById(R.id.currentUserImageView)
        val replyButton : ImageButton = itemView.findViewById(R.id.replyImageButton)
        val progressBar: ProgressBar = itemView.findViewById(R.id.chatListItemProgressBar)
        val chatImageView: ImageView = itemView.findViewById(R.id.chatImageView)

        fun setMessage(message: String?) {
            if (message != null) {
                messageField.text = message
                messageField.visibility = View.VISIBLE
            }else{
                messageField.visibility = View.GONE
            }
        }

        fun handleChatImage(chat: ChatMessage, glide: RequestManager, context: Context?) {
            val imageUrl = chat.chat_image_url
            val thumbUrl = chat.chat_thumb_url
            val imageStatus = chat.image_status
            when(imageStatus){
                IMAGE_STATUS_HAS_NO_IMAGE -> {
                    progressBar.visibility = View.GONE
                    chatImageView.visibility = View.GONE
                }
                IMAGE_STATUS_ERROR_UPLOADING -> {
                    chatImageView.visibility = View.VISIBLE
                    chatImageView.setImageDrawable(context?.getDrawable(R.drawable.ic_error))
                    //find better way to alert user
                }
                IMAGE_STATUS_IS_UPLOADING -> {
                    chatImageView.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                }
                IMAGE_STATUS_UPLOAD_SUCCESS -> {
                    if (imageUrl != null && thumbUrl != null) {
                        chatImageView.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                        /* todo animate loading image */
                        common.setImage(imageUrl, thumbUrl, chatImageView, glide)
                    }
                }else ->{
                Log.w(TAG, "chat image has no registered image status\nimage status is: $imageStatus")
                chatImageView.visibility = View.GONE
            }
            }
        }

        fun setUserData(username: String) {
            usernameField.text = username
        }

        fun handleUserViewVisibility(chat: ChatMessage, glide: RequestManager) {
            val userImageUrl : String? = chat.user_image_url
            if (common.uid() == chat.user_id) {
                userImageView.visibility = View.GONE
                currentUserImageView.visibility = View.VISIBLE
                userImageUrl?.let { common.setCircleImage(it, currentUserImageView, glide) }
            } else {
                userImageView.visibility = View.VISIBLE
                currentUserImageView.visibility = View.GONE
                userImageUrl?.let { common.setCircleImage(it, userImageView, glide) }
            }
        }
    }
}

