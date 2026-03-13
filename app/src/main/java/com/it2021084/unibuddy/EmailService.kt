package com.it2021084.unibuddy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailService {
    private const val SENDER_EMAIL = "unibuddy.application@gmail.com"
    private const val SENDER_PASSWORD = "njod owkz frsr ogjt"

    suspend fun sendEmail(
        recipientEmail: String,
        subject: String,
        body: String,
        replyToEmail: String
    ) : Boolean = withContext(Dispatchers.IO){
        try {
            val props = Properties().apply{
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
            }

            val session = Session.getInstance(props, object: Authenticator(){
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                }
            })

            val msg = MimeMessage(session).apply{
                setFrom(InternetAddress(SENDER_EMAIL, "UniBuddy App"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                setSubject(subject)
                setText(body)

                setReplyTo(arrayOf(InternetAddress(replyToEmail)))
            }

            Transport.send(msg)
            return@withContext true
        }catch (e: Exception){
            e.printStackTrace()
            return@withContext false
        }
    }
}