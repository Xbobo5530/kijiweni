package com.nyayozangu.labs.kijiweni.models

import com.google.firebase.Timestamp
import java.util.*

class ChatMessages(val message: String = "",
                   val image_url: String? = "",
                   var chat_id: String = "",
                   val username: String = "",
                   val user_image_url: String? = "",
                   val timestamp: Timestamp? = null,
                   val reply_source_id: String? = ""
)