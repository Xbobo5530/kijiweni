package com.nyayozangu.labs.kijiweni.helpers

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.nyayozangu.labs.kijiweni.R

const val TAG = "Sean"
const val CHATS = "Chats"
const val USERS = "Users"
const val MESSAGE = "message"
const val TIMESTAMP = "timestamp"
const val USERNAME = "username"
const val USER_ID = "user_id"
const val CHAT_IMAGE_URL = "chat_image_url"
const val CHAT_IMAGE_PATH = "chat_image_path"
const val CHAT_THUMB_URL = "chat_thumb_url"
const val CHAT_THUMB_PATH = "chat_thumb_path"
const val CHAT_IMAGES = "chat_images"
const val THUMBS = "thumbs"
const val USER_IMAGE_URL = "user_image_url"


object Common {

    fun mAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    fun database(): FirebaseFirestore = FirebaseFirestore.getInstance()
    fun storage(): FirebaseStorage = FirebaseStorage.getInstance()
    fun isLoggedIn(): Boolean = this.mAuth().currentUser != null
    fun uid(): String? = this.mAuth().currentUser?.uid
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
            Log.d(TAG, "error setting image\n + ${e.message}")
        }
    }

    fun setImage(imageUrl: String, mImageView: ImageView, glide: RequestManager) {
        try {
            val placeHolderRequest = RequestOptions()
            placeHolderRequest.placeholder(R.drawable.ic_image)
            glide.applyDefaultRequestOptions(placeHolderRequest)
                    .load(imageUrl).into(mImageView)
        } catch (e: Exception) {
            Log.d(TAG, "error setting image\n + ${e.message}")
        }
    }
    fun setImage(imageUrl: String, thumbUrl: String,mImageView: ImageView,
                 glide: RequestManager, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        try {
            val placeHolderRequest = RequestOptions()
            placeHolderRequest.placeholder(R.drawable.ic_image)
            glide.applyDefaultRequestOptions(placeHolderRequest)
                    .load(imageUrl)
                    .thumbnail(glide.load(thumbUrl))
                    .into(mImageView)
            progressBar.visibility = View.GONE

        } catch (e: Exception) {
            Log.d(TAG, "error setting image\n + ${e.message}")
        }
    }

    fun setImageByPath(imagePath: String, thumbPath: String,mImageView: ImageView,
                 glide: RequestManager, progressBar: ProgressBar) {
        progressBar.visibility = View.VISIBLE
        val imagePathRef = this.storage().reference.child(imagePath)
        val thumbPathRef = this.storage().reference.child(thumbPath)
        try {
            val placeHolderRequest = RequestOptions()
            placeHolderRequest.placeholder(R.drawable.ic_image)
            glide/*.using(FirebaseImageLoader())*/
                    .applyDefaultRequestOptions(placeHolderRequest)
                    .load(imagePathRef)
                    .thumbnail(glide.load(thumbPathRef))
                    .into(mImageView)
            progressBar.visibility = View.GONE

        } catch (e: Exception) {
            Log.d(TAG, "error setting image\n + ${e.message}")
        }
    }
}