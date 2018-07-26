package com.nyayozangu.labs.kijiweni.helpers

import android.view.View
import android.widget.ProgressBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object Common {

    fun mAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    fun database(): FirebaseFirestore = FirebaseFirestore.getInstance()
    fun isLoggedIn(): Boolean = this.mAuth().currentUser != null
    fun currentUser(): FirebaseUser? = this.mAuth().currentUser
    fun showProgress(progressBar: ProgressBar){
        progressBar.visibility = View.VISIBLE
    }
    fun stopLoading(progressBar: ProgressBar){
        progressBar.visibility = View.GONE
    }
}