package com.nyayozangu.labs.kijiweni.activities

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.nyayozangu.labs.kijiweni.R
import com.nyayozangu.labs.kijiweni.adapters.ChatRecyclerViewAdapter
import com.nyayozangu.labs.kijiweni.models.ChatMessage
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.collections.HashMap
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.*
import com.theartofdev.edmodo.cropper.CropImage
import kotlin.collections.ArrayList
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.nyayozangu.labs.kijiweni.helpers.*
import id.zelory.compressor.Compressor
import org.jetbrains.anko.AnkoAsyncContext
import org.jetbrains.anko.alert
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*
import java.util.Calendar.HOUR_OF_DAY

private const val RC_SIGN_IN = 0
private const val PERMISSION_WRITE_EXTERNAL_STORAGE = 13

class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterCallback {

    private val common: com.nyayozangu.labs.kijiweni.helpers.Common = Common
    private val login: LoginHelper = LoginHelper()
    private val chatList: MutableList<ChatMessage> = ArrayList()
    private lateinit var mAdapter: ChatRecyclerViewAdapter
    private val database = FirebaseFirestore.getInstance()
    private val chatRef = database.collection(CHATS)
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var userImageUrl: String
    private lateinit var username: String
    private lateinit var userId: String
    private var chatImageUri: Uri? = null
    private lateinit var timeLimit: GregorianCalendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleLoginPreps()
        FirebaseMessaging.getInstance().subscribeToTopic(UPDATES)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setHomeAsUpIndicator(R.drawable.ic_app_icon)

        mRecyclerView = this.findViewById(R.id.chatRecyclerView)
        mAdapter = ChatRecyclerViewAdapter(chatList, Glide.with(this), this)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.adapter = mAdapter
        timeLimit = GregorianCalendar().apply { set(HOUR_OF_DAY, - 24) }

        if (common.isLoggedIn()) {
            val user = common.currentUser()!!
            username = user.displayName.toString()
            userId = user.uid
            userImageUrl = user.photoUrl.toString()
            handleIntent()
            loadChats()
        }else{
            signIn()
        }
        sendImageButton.setOnClickListener(this)
        addImageImageButton.setOnClickListener(this)
    }


    override fun reply(chatId: String?) {
        Log.d(TAG, "reply is clicked, chat id is $chatId")
    }

    private fun handleIntent() {
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null){
            when {
                "text/plain" == type -> handleSendText(intent)
                type.startsWith("image/") -> handleSendImage(intent)
            }
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.sendImageButton -> {
                sendMessage()
            }
            R.id.addImageImageButton  -> handleAddImage()
            else -> {
                Log.d(TAG, "clicked some other button ${v?.id}")
            }
        }
    }

    private fun handleAddImage() {

        if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
            requestStoragePermissions()
        }else{
            CropImage.activity()
                    .setCropMenuCropButtonIcon(R.drawable.ic_send_white)
                    .start(this)
        }
    }

    private fun requestStoragePermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            //add additional rational for the user

            Snackbar.make(mainView,"Storage access permissions are required to upload/download files.",
                    Snackbar.LENGTH_LONG)

        }else{
            // Permission has not been granted yet. Request it directly.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSION_WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun handleSendImage(intent: Intent?) {
        val imageUri: Uri? = intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        //handle shared image
        imageUri?.let { startImageCropper(it) }
    }

    private fun handleSendText(intent: Intent?) {
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        chatFieldEditText.setText(sharedText)
    }

    private fun handleLoginPreps() {
        val gso = login.gso(this)
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun sendMessage() {
        val message = chatFieldEditText.text.toString().trim()
        val chatMap: HashMap<String, Any?> = createBaseMap()
        if (!message.isEmpty()) {
            chatMap[MESSAGE] = message
            addMessageToDb(chatMap)
        }
        chatFieldEditText.text.clear()
    }

    private fun addMessageToDb(chatMap: HashMap<String, Any?>) {
        chatMap[IMAGE_STATUS] = IMAGE_STATUS_HAS_NO_IMAGE
        chatRef.add(chatMap)
                .addOnSuccessListener {
                    common.sendNotification(NEW_TEXT_MESSAGE, it.id)
                }
                .addOnFailureListener{
                    Log.d(TAG, "failed to add message to database: ${it.message}")
                    showSnack("${getString(R.string.error_text)}: ${it.message}")
                }
    }

    private fun uploadChatImage(chatMap: HashMap<String, Any?>) {
        doAsync {
            val randomId = UUID.randomUUID().toString()
            val filePath = common.storage().reference.child(CHAT_IMAGES).child("$randomId.jpg")
            chatImageUri?.let { uri ->
                filePath.putFile(uri).addOnSuccessListener { snapshot ->
                    val downloadUri = snapshot.downloadUrl
                    chatMap[CHAT_IMAGE_URL] = downloadUri.toString()
                    val newImageFile = File(chatImageUri!!.path)
                    compressImageFile(newImageFile, randomId, chatMap)
                }
            }
        }
    }

    private fun AnkoAsyncContext<MainActivity>.compressImageFile(newImageFile: File, randomId: String, chatMap: HashMap<String, Any?>) {
        try {
            val compressedImageFile = Compressor(this@MainActivity)
                    .setMaxWidth(100)
                    .setMaxHeight(100)
                    .setQuality(2)
                    .compressToBitmap(newImageFile)
            val baos = ByteArrayOutputStream()
            compressedImageFile.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val thumbData = baos.toByteArray()
            common.storage().reference.child("$CHAT_IMAGES/$THUMBS")
                    .child("$randomId.jpg")
                    .putBytes(thumbData)
                    .addOnSuccessListener {
                        val thumbDownloadUrl = it.downloadUrl
                        chatMap[CHAT_THUMB_URL] = thumbDownloadUrl.toString()
                        chatMap[IMAGE_STATUS] = IMAGE_STATUS_UPLOAD_SUCCESS
                        addChatMessageWithImage(chatMap)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "failed to update message ${exception.message}")
                        alertUserUploadFailed(exception)
                    }
        } catch (e: IOException) {
            Log.e(TAG, "Error compressing a file${e.message}")
        }
    }

    private fun AnkoAsyncContext<MainActivity>.alertUserUploadFailed(exception: Exception) {
        uiThread {
            alert ("${getString(R.string.failed_to_upload_image_text)}\n${exception.message}",
                    getString(R.string.error_text)) {
                icon = getDrawable(R.drawable.ic_error)
                positiveButton(getString(R.string.retry_text)) { dialog -> dialog.dismiss()
                    chatImageUri?.let { uri -> startImageCropper(uri) }
                }
                negativeButton(getString(R.string.cancel_text)) { dialog -> dialog.dismiss() }
            }.show()
        }
    }

    private fun startImageCropper(chatImageUri: Uri) {
        CropImage.activity(chatImageUri)
                .setCropMenuCropButtonIcon(R.drawable.ic_send_white)
                .start(this)
    }

    private fun addChatMessageWithImage(chatMap: HashMap<String, Any?>) {
        chatRef.add(chatMap).addOnSuccessListener {
            val chatId = it.id
            common.sendNotification(NEW_IMAGE_MESSAGE, chatId)
        }
                .addOnFailureListener{
                    Log.e(TAG, "filed to update chat message after uploading image: ${it.message}")
                    showSnack("${getString(R.string.error_text)}: ${it.message}")
                }
        chatImageUri = null //remove the image uri

    }

    private fun showSnack(message: String) {
        Snackbar.make(mainView, message, Snackbar.LENGTH_LONG).show()
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
        common.showProgress(progressBar)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){
            RC_SIGN_IN ->  handleGoogleSignIn(data)
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> handleImagePicked(data, resultCode)
            else -> {
                val errorMessage = getString(R.string.something_went_wrong)
                showSnack(errorMessage)
            }
        }
    }

    private fun handleImagePicked(data: Intent?, resultCode: Int) {
        val result = CropImage.getActivityResult(data)
        when (resultCode){
            Activity.RESULT_OK -> {
                chatImageUri = result.uri
                val chatMap: HashMap<String, Any?> = createBaseMap()
                uploadChatImage(chatMap)
                showSnack("Your image is being uploaded")
            }
            CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
                selectedImageImageView.visibility = View.GONE
                cancelAddImageButton.visibility = View.GONE
                val error = result.error
                chatImageUri = null
                showSnack("${getString(R.string.error_text)}: ${error.message}")
            }else -> {
            chatImageUri = null
            selectedImageImageView.visibility = View.GONE
            cancelAddImageButton.visibility = View.GONE
        }
        }
    }

    private fun createBaseMap(): HashMap<String, Any?> {
        val timestamp = FieldValue.serverTimestamp()
        return hashMapOf(
                CREATED_AT to timestamp,
                USERNAME to username,
                USER_ID to userId,
                USER_IMAGE_URL to userImageUrl
        )
    }

    private fun handleGoogleSignIn(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        if (task.isSuccessful) {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } else {
            val errorMessage = "Google sign in failed: ${task.exception?.message}"
            Log.w(TAG, errorMessage)
            if (!task.exception?.message?.contains("12502")!!) showSnack(errorMessage)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle: ${ account.id!!}")
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        common.mAuth().signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        userImageUrl = account.photoUrl.toString()
                        username = account.displayName.toString()
                        userId = account.id.toString()
                        Log.d(TAG, "signInWithCredential:success")
                        processUser()
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        val errorMessage = "Authentication Failed: ${task.exception}"
                        promptRetryLogin(errorMessage)
                    }
                }
    }

    private fun processUser() {
        val userRef = common.database().collection(USERS)
        userRef.document(userId).get().addOnSuccessListener {
            if (it.exists()){
                //user exists process
                showSnack("${getString(R.string.welcome_back_text)} $username")
                loadChats()
            }else{
                addUser(userRef)
            }
        }
                .addOnFailureListener{ exception ->
                    val errorMessage = "${getString(R.string.error_text)}: ${exception.message}"
                    Log.e(TAG, "failed to process user ${exception.message}")
                    common.stopLoading(progressBar)

                    alert (errorMessage, getString(R.string.error_text) ){
                        icon = getDrawable(R.drawable.ic_error)
                        positiveButton (getString(R.string.retry_text)) {processUser() }
                        negativeButton(getString(R.string.cancel_text)) {dialog -> dialog.cancel() }
                    }.show()
                }
    }

    private fun addUser(userRef: CollectionReference) {
        val timestamp = FieldValue.serverTimestamp()
        val userMap: Map<String, Any> = hashMapOf(
                USER_ID to userId,
                USERNAME to username,
                USER_IMAGE_URL to userImageUrl,
                CREATED_AT to timestamp
        )
        userRef.document(userId).set(userMap).addOnSuccessListener {
            Log.d(TAG, "user added")
            loadChats()
            common.stopLoading(progressBar)
            showSnack("${getString(R.string.welcome_text)} $username")
        }
                .addOnFailureListener{
                    Log.e(TAG, "failed to add user: ${it.message}", it)
                    val errorMessage = "${getString(R.string.failed_to_signin)}: ${it.message}"
                    common.stopLoading(progressBar)
                    promptRetryLogin(errorMessage)
                }
    }

    private fun promptRetryLogin(errorMessage: String) {
        alert (errorMessage, getString(R.string.error_text)) {
            icon = getDrawable(R.drawable.ic_error)
            positiveButton(getString(R.string.retry_text)){signIn()}
            negativeButton(getString(R.string.cancel_text)){dialog -> dialog.dismiss() }
        }.show()
    }

    private fun loadChats() {
        chatRef.whereGreaterThan(CREATED_AT, timeLimit.time)
                .orderBy(CREATED_AT, Query.Direction.ASCENDING)
                .addSnapshotListener { querySnapshot, exception ->
            if (exception == null && querySnapshot != null){
                if (!querySnapshot.isEmpty){
                    for (mDocument in querySnapshot.documentChanges){
                        when {
                            mDocument.type == DocumentChange.Type.ADDED -> {
                                val chatId = mDocument.document.id
                                val chat = mDocument.document.toObject(ChatMessage::class.java)
                                chat.chat_id = chatId
                                chatList.add(chat)
                                mAdapter.notifyItemInserted(chatList.size - 1)
                                common.stopLoading(progressBar)
                            }
                        }
                            mRecyclerView.scrollToPosition(chatList.size - 1)
                    }
                }else{
                    Log.d(TAG, "No messages")
                }
            }else{
                Log.d(TAG, "error getting chats: ${exception?.message}")
            }
        }
    }


}