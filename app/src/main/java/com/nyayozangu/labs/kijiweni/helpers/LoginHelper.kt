package com.nyayozangu.labs.kijiweni.helpers

import android.app.Activity
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.nyayozangu.labs.kijiweni.R

class LoginHelper {

    private val common: Common = Common

    fun gso(context: Context): GoogleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()

}