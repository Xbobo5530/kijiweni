package com.nyayozangu.labs.kijiweni.activities

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nyayozangu.labs.kijiweni.R
import com.nyayozangu.labs.kijiweni.adapters.ChatRecyclerViewAdapter
import com.nyayozangu.labs.kijiweni.models.ChatMessages
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.Query
import com.nyayozangu.labs.kijiweni.helpers.Common
import com.nyayozangu.labs.kijiweni.helpers.LoginHelper
import kotlin.collections.ArrayList


private const val TAG = "Sean"
private const val CHATS = "Chats"
private const val MESSAGE = "message"
private const val TIMESTAMP = "timestamp"
private const val USERNAME = "username"
private const val USER_ID = "user_id"
private const val IMAGE_URL = "image_url"
private const val USER_IMAGE_URL = "user_image_url"
private const val RC_SIGN_IN = 0

private const val USERS = "Users"

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val common: com.nyayozangu.labs.kijiweni.helpers.Common = Common
    private val login: LoginHelper = LoginHelper()
    private val chatList: MutableList<ChatMessages> = ArrayList()
    private lateinit var mAdapter: ChatRecyclerViewAdapter
    private val database = FirebaseFirestore.getInstance()
    private val chatRef = database.collection(CHATS)
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var userImageUrl: String
    private lateinit var username: String
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleLoginPreps()
        checkLoginStatus()

        if (common.isLoggedIn()) {
            val user = common.currentUser()!!
            username = user.displayName.toString()
            userId = user.uid
            userImageUrl = user.photoUrl.toString()

            setContentView(R.layout.activity_main)
            mRecyclerView = findViewById(R.id.chatRecyclerView)
            mAdapter = ChatRecyclerViewAdapter(chatList, Glide.with(this), this)
            mRecyclerView.layoutManager = LinearLayoutManager(this)
            mRecyclerView.adapter = mAdapter
            handleIntent()
            loadChats()
            sendImageButton.setOnClickListener(this)
        }
    }

    private fun handleIntent() {
        val intent = intent
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null){
            when {
                "text/plain" == type -> handleSendText(intent)
                type.startsWith("image/") -> handleSendImage(intent)
                else -> {
                    //handle other types of intents
                }
            }
        }
    }

//    override fun onStart() {
//        super.onStart()
//        checkLoginStatus()
//    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.sendImageButton -> handleSendMessage()
        }
    }

    private fun handleSendImage(intent: Intent?) {
        val imgaeUri: Uri? = intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        //handle shared image
    }

    private fun handleSendText(intent: Intent?) {
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        chatFieldEditText.setText(sharedText)
    }

    private fun handleLoginPreps() {
        val gso = login.gso(this)
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun handleSendMessage() {
        if(!chatFieldEditText.text.toString().isEmpty()){
            val chatMessage = chatFieldEditText.text.toString()
            val timestamp = FieldValue.serverTimestamp()
            val chatMap: HashMap<String, Any> = hashMapOf(
                    MESSAGE to chatMessage,
                    TIMESTAMP to timestamp,
                    USERNAME to username,
                    USER_ID to userId,
                    USER_IMAGE_URL to userImageUrl
            )
            chatRef.add(chatMap).addOnSuccessListener {
                Log.d(TAG, "message added to database")
            }
                    .addOnFailureListener{
                        val errorMessage = "failed to add message to database: ${it.message}"
                        Log.d(TAG, errorMessage)
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
            chatFieldEditText.text.clear()
        }
    }

    private fun checkLoginStatus() {
        if(!common.isLoggedIn()){
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
        common.showProgress(progressBar)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            }else{
                val errorMessage = "Google sign in failed: ${task.exception?.message}"
                Log.w(TAG, errorMessage)
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
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
                        addUserToDB()
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        val errorMessage = "Authentication Failed: ${task.exception}"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
    }

    private fun addUserToDB() {
        val userRef = common.database().collection(USERS)
        val userMap: Map<String, Any> = HashMap()
        userMap.entries
        userRef.document(userId).set(userMap).addOnSuccessListener {
            Log.d(TAG, "user added")
            common.stopLoading(progressBar)
        }
                .addOnFailureListener{
                    Log.e(TAG, "failed to add user: ${it.message}", it)
                    val errorMessage = "${getString(R.string.failed_to_signin)}: ${it.message}"
                    AlertDialog.Builder(this).apply {
                        this.setTitle(getString(R.string.error_text))
                                .setMessage(errorMessage)
                                .setNegativeButton(getString(R.string.cancel_text)
                                ) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(getString(R.string.retry_text)) { _, _ ->
                                    signIn()
                                }
                                .setCancelable(false)
                                .show()
                    }
                }
    }

    private fun loadChats() {
        chatRef.orderBy(TIMESTAMP, Query.Direction.ASCENDING)
                .addSnapshotListener { querySnapshot, exception ->
            if (exception == null){
                if (!querySnapshot?.isEmpty!!){
                    for (mDocument in querySnapshot.documentChanges){
                        if (mDocument.type == DocumentChange.Type.ADDED){
                            val chatId = mDocument.document.id
                            val chat = mDocument.document.toObject(ChatMessages::class.java)
                            chat.chat_id = chatId
                            chatList.add(chat)
                            mAdapter.notifyDataSetChanged()
                            mRecyclerView.smoothScrollToPosition(chatList.size - 1)
                        }
                    }
                }
            }else{
                Log.d(TAG, "error getting chats: ${exception.message}")
            }
        }
    }
}