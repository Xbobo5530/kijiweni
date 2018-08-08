package com.nyayozangu.labs.kijiweni.helpers

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.nyayozangu.labs.kijiweni.R
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

const val TAG = "Sean"
const val CHATS = "Chats"
const val USERS = "Users"
const val MESSAGE = "message"
const val UPDATES = "updates"
const val CREATED_AT = "created_at"
const val USERNAME = "username"
const val USER_ID = "user_id"
const val CHAT_ID = "chat_id"
const val IMAGE_URL = "imageUrl"
const val CHAT_IMAGE_URL = "chat_image_url"
const val CHAT_IMAGE_PATH = "chat_image_path"
const val CHAT_THUMB_URL = "chat_thumb_url"
const val CHAT_THUMB_PATH = "chat_thumb_path"
const val CHAT_IMAGES = "chat_images"
const val THUMBS = "thumbs"
const val USER_IMAGE_URL = "user_image_url"
const val IMAGE_STATUS = "image_status"
const val IMAGE_STATUS_HAS_NO_IMAGE = 0
const val IMAGE_STATUS_IS_UPLOADING = 1
const val IMAGE_STATUS_ERROR_UPLOADING = 2
const val IMAGE_STATUS_UPLOAD_SUCCESS = 3
const val NEW_IMAGE_MESSAGE = "new_image_message"
const val NEW_TEXT_MESSAGE = "new_text_message"
const val CHAT_MESSAGE_TYPE = "chat_message_type"

const val API_KEY = "key=AAAAS9KGWkY:APA91bGR3nhfrl0nvh_0BYjZcRrd4bpx8QKuEXfWl_mvC90caPbOQKUGKZGXls1f05SxSP1RcKWYI4RIn61n8ZU5ZBpuZMKC_0oobJnuxTRnxzW8nsxySxUQAgboGziz8LRD22gT5TTLPfvGg47LE9hokT-BYBG8QQ"


object Common {

    fun mAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    fun database(): FirebaseFirestore = FirebaseFirestore.getInstance()
    fun storage(): FirebaseStorage = FirebaseStorage.getInstance()
    fun isLoggedIn(): Boolean = this.mAuth().currentUser != null
    fun currentUserUid(): String? = this.mAuth().currentUser?.uid
    fun currentUser(): FirebaseUser? = this.mAuth().currentUser
    fun showProgress(progressBar: ProgressBar){
        progressBar.visibility = View.VISIBLE
    }
    fun stopLoading(progressBar: ProgressBar){
        progressBar.visibility = View.GONE
    }

    fun setCircleImage(userImageUrl: String, mImageView: ImageView, glide: RequestManager) {
        try {
            val placeHolderRequest = RequestOptions()
            placeHolderRequest.placeholder(R.drawable.ic_user)
            glide.applyDefaultRequestOptions(placeHolderRequest.circleCrop())
                    .load(userImageUrl).into(mImageView)
        } catch (e: Exception) {
            Log.d(TAG, "error setting image\n ${e.message}")
        }
    }

    fun setImage(imageUrl: String, mImageView: ImageView, glide: RequestManager) {
        try {
            val placeHolderRequest = RequestOptions()
            placeHolderRequest.placeholder(R.color.colorWhite)
            glide.applyDefaultRequestOptions(placeHolderRequest.fitCenter())
                    .load(imageUrl).into(mImageView)
        } catch (e: Exception) {
            Log.d(TAG, "error setting image\n + ${e.message}")
        }
    }
    fun setImage(imageUrl: String, thumbUrl: String, mImageView: ImageView,
                 glide: RequestManager) {
        try {
            val placeHolderRequest = RequestOptions()
            placeHolderRequest.placeholder(R.color.colorWhite)
            glide.applyDefaultRequestOptions(placeHolderRequest.fitCenter())
                    .load(imageUrl)
                    .transition(withCrossFade())
                    .thumbnail(glide.load(thumbUrl))
                    .into(mImageView)
        } catch (e: Exception) {
            Log.d(TAG, "error setting image\n + ${e.message}")
        }
    }

    fun sendNotification(type: String, chatId: String){
        doAsync {
            try {
                var topic = "/topics/"
                val url = URL("https://fcm.googleapis.com/fcm/send")
                val connection = url.openConnection() as HttpURLConnection
                connection.useCaches = false
                connection.doInput = true
                connection.doOutput = true

                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", API_KEY)
                connection.setRequestProperty("Content-Type", "application/json")

                val json = JSONObject()
                val info = JSONObject()

                topic += UPDATES
                when (type) {
                    NEW_IMAGE_MESSAGE -> {
//                        topic += NEW_IMAGE_MESSAGE
                        info.put(CHAT_MESSAGE_TYPE, NEW_IMAGE_MESSAGE)
                    }
                    NEW_TEXT_MESSAGE -> {
//                        topic += NEW_TEXT_MESSAGE
                        info.put(CHAT_MESSAGE_TYPE, NEW_TEXT_MESSAGE)
                    }
                }
                json.put("to", topic)
                info.put(CHAT_ID, chatId)
                json.put("data", info)
                val wr = OutputStreamWriter(connection.outputStream)
                wr.write(json.toString())
                wr.flush()
                connection.inputStream
                Log.d(TAG, "notification sent.\ntype is $type, id is $chatId")

            }catch (e: Exception){
                Log.e(TAG, "error when sending notification: ${e.message}")
            }
        }
    }
}