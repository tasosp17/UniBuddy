package com.it2021084.unibuddy

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object FcmTokenSender{
    //reads service_account.json and gets a fresh access token
    suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO){
        try{
            val stream = context.assets.open("service_account.json")
            val googleCredentials = GoogleCredentials.fromStream(stream)
                .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

            googleCredentials.refreshIfExpired()
            googleCredentials.accessToken.tokenValue
        }catch (e: IOException){
            e.printStackTrace()
            null
        }
    }
}