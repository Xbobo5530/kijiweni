package com.nyayozangu.labs.kijiweni.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.nyayozangu.labs.kijiweni.R
import com.nyayozangu.labs.kijiweni.helpers.Common
import com.nyayozangu.labs.kijiweni.helpers.IMAGE_URL
import kotlinx.android.synthetic.main.activity_view_image.*

class ViewImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_image)
        setSupportActionBar(viewImageToolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = getString(R.string.app_name)
        viewImageToolbar.setNavigationOnClickListener {
            finish()
        }
        handleIntent()
    }

    private fun handleIntent(){
        val intent = intent
        val imageUrl = intent.getStringExtra(IMAGE_URL)
        val common = Common
        imageUrl?.let { common.setImage(it, imageView, Glide.with(this)) }
    }
}