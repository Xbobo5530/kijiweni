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
import com.nyayozangu.labs.kijiweni.activities.ViewImageActivity
import com.nyayozangu.labs.kijiweni.helpers.*
import com.nyayozangu.labs.kijiweni.models.ChatMessage
import org.jetbrains.anko.startActivity

const val VIEW_TYPE_SENT_MESSAGE = 0
const val VIEW_TYPE_RECEIVED_MESSAGE = 1

private val common: Common = Common
//todo add two times of view types for the send and received messages
class ChatRecyclerViewAdapter(private val chatList: List<ChatMessage>,
                              private val glide: RequestManager,
                              private var context: Context?,
                              private val callback: AdapterCallback?):
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when(viewType) {
            VIEW_TYPE_SENT_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.sent_chat_list_item, parent, false)
                this.context = parent.context
                return SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.received_chat_list_item, parent, false)
                this.context = parent.context
                return ReceivedMessageViewHolder(view)
            }else -> {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.received_chat_list_item, parent, false)
            this.context = parent.context
            return ReceivedMessageViewHolder(view)
        }
        }
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType){
            VIEW_TYPE_SENT_MESSAGE -> context?.let { (holder as SentMessageViewHolder).bind(chatList[position], glide, it) }
            VIEW_TYPE_RECEIVED_MESSAGE -> context?.let { (holder as ReceivedMessageViewHolder).bind(chatList[position], glide, it) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chat = chatList[position]
        val currentUserId = common.currentUser()?.uid
        val chatUserId = chat.user_id
        return if (currentUserId.equals(chatUserId)){
            VIEW_TYPE_SENT_MESSAGE
        }else{
            VIEW_TYPE_RECEIVED_MESSAGE
        }
    }

    private fun handleReplyMessage(callback: AdapterCallback?, chatId: String?) {
        //pass the chat id to the main activity and handle reply from there
        callback?.reply(chatId)
        Log.d(TAG, "reply button clicked in adapter")
    }

    //todo add a new view holder for the sent and received messages

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val messageField: TextView = itemView.findViewById(R.id.sentMessageTextView)
        private val replyButton : ImageButton = itemView.findViewById(R.id.sentMessageReplyImageButton)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.sentMessageChatListItemProgressBar)
        private val chatImageView: ImageView = itemView.findViewById(R.id.sentMessageImageView)

        fun bind(chat: ChatMessage, glide: RequestManager, context: Context){
            val message = chat.message
            if (message == null || message.isEmpty()){
                messageField.visibility = View.GONE
            }else {
                messageField.text = message
                messageField.visibility = View.VISIBLE
            }
            //handle image
            val imageUrl = chat.chat_image_url
            val thumbUrl = chat.chat_thumb_url
            if (imageUrl != null && thumbUrl != null){
                val imageStatus = chat.image_status
                when (imageStatus){
                    IMAGE_STATUS_HAS_NO_IMAGE -> {
                        common.stopLoading(progressBar)
                        chatImageView.visibility = View.GONE
                    }
                    IMAGE_STATUS_UPLOAD_SUCCESS -> {
                        common.stopLoading(progressBar)
                        common.setImage(imageUrl, thumbUrl, chatImageView, glide)
                        chatImageView.setOnClickListener {
                            context.startActivity<ViewImageActivity>(IMAGE_URL to imageUrl)
                        }
                        chatImageView.visibility = View.VISIBLE
                    }
                    IMAGE_STATUS_IS_UPLOADING -> {
                        common.showProgress(progressBar)
                        chatImageView.visibility = View.GONE
                    }
                    IMAGE_STATUS_ERROR_UPLOADING -> {
                        common.stopLoading(progressBar)
                        chatImageView.setImageDrawable(context.getDrawable(R.drawable.ic_error))
                    }
                    else -> {
                        common.stopLoading(progressBar)
                        chatImageView.visibility = View.GONE
                    }
                }
            }
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val usernameField: TextView = itemView.findViewById(R.id.receivedMessageUsernameTextView)
        private val userImageView: ImageView = itemView.findViewById(R.id.receivedMessageUserImageView)
        private val messageField: TextView = itemView.findViewById(R.id.receivedMessageTextView)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.receivedMessageChatListItemProgressBar)
        private val chatImageView: ImageView = itemView.findViewById(R.id.receivedMessageImageView)
        private val replyButton: ImageButton = itemView.findViewById(R.id.sentMessageReplyImageButton)

        fun bind(chat: ChatMessage, glide: RequestManager, context: Context) {
            //set user data
            val username = chat.username
            usernameField.text = username
            val userImageUrl = chat.user_image_url
            userImageUrl?.let { common.setCircleImage(it, userImageView, glide) }
            //handle image
            val imageUrl = chat.chat_image_url
            val thumbUrl = chat.chat_thumb_url
            if (imageUrl != null && thumbUrl != null){
                val imageStatus = chat.image_status
                when (imageStatus){
                    IMAGE_STATUS_HAS_NO_IMAGE -> {
                        common.stopLoading(progressBar)
                        chatImageView.visibility = View.GONE
                    }
                    IMAGE_STATUS_UPLOAD_SUCCESS -> {
                        common.stopLoading(progressBar)
                        chatImageView.visibility = View.VISIBLE
                        common.setImage(imageUrl, thumbUrl, chatImageView, glide)
                        chatImageView.setOnClickListener {
                            context.startActivity<ViewImageActivity>(IMAGE_URL to imageUrl)
                        }
                    }
                    IMAGE_STATUS_IS_UPLOADING -> {
                        common.showProgress(progressBar)
                        chatImageView.visibility = View.GONE
                    }
                    IMAGE_STATUS_ERROR_UPLOADING -> {
                        common.stopLoading(progressBar)
                        chatImageView.setImageDrawable(context.getDrawable(R.drawable.ic_error))
                    }
                    else -> {
                        common.stopLoading(progressBar)
                        chatImageView.visibility = View.GONE
                    }
                }
            }
            //handle message
            val message = chat.message
            if (message == null || message.isEmpty()){
                messageField.visibility = View.GONE
            }else {
                messageField.text = message
                messageField.visibility = View.VISIBLE
            }
        }
    }
}

