package com.nyayozangu.labs.kijiweni.models

import com.google.firebase.Timestamp
import java.util.*

class ChatMessage(val message: String = "",
                  val chat_image_url: String? = null,
                  val chat_thumb_url: String? = null,
                  var chat_id: String? = null,
                  val username: String = "",
                  val user_id: String = "",
                  val user_image_url: String? = null,
                  val timestamp: Timestamp? = null,
                  val reply_source_id: String? = null
)