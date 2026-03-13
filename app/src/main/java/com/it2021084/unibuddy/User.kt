package com.it2021084.unibuddy

import com.google.firebase.database.PropertyName

data class User(
    var uid: String = "",
    var name: String? = null,
    var email: String? = null,
    var status: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = false,
    var avatar: String? = null, //base64 string
    var enrolledCourses: HashMap<String, Boolean> = HashMap(), //store user attending courses
    var ssid: String = "",
    var visibilityMode: Int = 2, //0=none, 1=best buddies, 2=all (default)
    var fcmToken: String = "",
    var bestBuddies: HashMap<String, Boolean> = HashMap()


)