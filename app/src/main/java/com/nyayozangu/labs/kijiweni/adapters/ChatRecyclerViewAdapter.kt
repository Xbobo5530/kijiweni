package com.nyayozangu.labs.kijiweni.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.nyayozangu.labs.kijiweni.R
import com.nyayozangu.labs.kijiweni.models.ChatMessages


class ChatRecyclerViewAdapter(private val chatList: List<ChatMessages>,
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
        val imageUrl = chat.image_url
        holder.usernameField.text = username
        holder.messageField.text = message

        val mAuth = FirebaseAuth.getInstance()

        if (mAuth.currentUser?.uid == chat.user_id) {
            userImageUrl?.let { setImage(it, holder.currentUserImageView) }
        }else{
            userImageUrl?.let { setImage(it, holder.userImageView) }
        }

        if (imageUrl != null){
            //show the image view for image
//            setImage(imageUrl, holder)
        }else{
            //hide the image view for image
        }
        holder.replyButton.setOnClickListener{handleReplyMessage()}
    }

    private fun handleReplyMessage() {
        //pass the chat id to the main activity and handle reply from there
    }

    private fun setImage(userImageUrl: String, mImageView: ImageView) {
        try {
            val placeHolderRequest = RequestOptions()
            placeHolderRequest.placeholder(R.drawable.ic_user)
            glide.applyDefaultRequestOptions(placeHolderRequest)
                    .load(userImageUrl).into(mImageView)
        } catch (e: Exception) {
            Log.d("adapter", "error setting image\n" + e.message)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val usernameField: TextView = itemView.findViewById(R.id.usernameTextView)
        val messageField: TextView = itemView.findViewById(R.id.chatMessageTextView)
        val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
        val currentUserImageView: ImageView = itemView.findViewById(R.id.currentUserImageView)
        val replyButton : ImageButton = itemView.findViewById(R.id.replyImageButton)
    }
}
