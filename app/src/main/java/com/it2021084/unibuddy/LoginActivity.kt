package com.it2021084.unibuddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.widget.Button
import androidx.compose.animation.core.snap
import com.google.firebase.database.FirebaseDatabase


class LoginActivity : ComponentActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?){

        super.onCreate(savedInstanceState)
        //checking if user is already signed it before loading UI
        val user = FirebaseAuth.getInstance().currentUser
        if(user != null){
            //check local cache, if SSID is saved locally > continue
            val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val cachedSsid = prefs.getString("LOCAL_SSID", null)

            if (!cachedSsid.isNullOrEmpty()){
                startMainActivity()
                return //stop here and don't load login screen
            }
            //if cache is empty, show UI and check SSID
            setContentView(R.layout.activity_login)
            checkSsidAndRedirect(user.uid)
            return
        }

        //load UI normally in case user is not logged in
        setContentView(R.layout.activity_login)

        //configure google sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) //use web client ID
            .requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleButton = findViewById<Button>(R.id.btnGoogleSignIn)
        googleButton.setOnClickListener {
            signInWithGoogle()
        }

        //testing
        val testLoginButton = findViewById<Button>(R.id.btnTestLogin)
        testLoginButton.setOnClickListener {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(
                "bob@test.com",
                "123456"
            ).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkSsidAndRedirect(FirebaseAuth.getInstance().uid!!)
                }
            }
        }
        testLoginButton.visibility = View.GONE

    }

    //sign in with google account
    private fun signInWithGoogle(){
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)!!
                val idToken = account.idToken
                if (idToken != null){
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: Exception){
                Log.e("LoginActivity", "Google sign-in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String){
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful){
                    //display successful connection
                    Log.d("LoginActivity", "Firebase Auth successful")
                    val user = FirebaseAuth.getInstance().currentUser
                    Log.d("LoginActivity", "Signed in as: ${user?.email}")

                    //initialize user in realtime database
                    val db = FirebaseDatabase.getInstance( "https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app").reference.child("users")
                    user?.let{ u ->
                        val uid = u.uid
                        val userRef = db.child(uid)
                        //ensure user node exists before navigating
                        userRef.get().addOnSuccessListener { snapshot ->
                            if (!snapshot.exists()){
                                val newUser = User(
                                    uid = uid,
                                    name = u.displayName ?: "",
                                    email = u.email ?: "",
                                    status = "",
                                    isActive = false,
                                    ssid = "",
                                    visibilityMode = 2
                                )
                                userRef.setValue(newUser).addOnSuccessListener {
                                    //new user created, SSID empty so go to settings
                                    startSettingsActivity()
                                }
                            } else {
                                //user exists, check SSID
                                checkSsidAndRedirect(uid)
                            }
                        }

                    }

                }
            }
    }

    //router function
    private fun checkSsidAndRedirect(uid: String){
        val db = FirebaseDatabase.getInstance("https://uni-buddy-it2021084-default-rtdb.europe-west1.firebasedatabase.app").getReference("users")

        db.child(uid).child("ssid").get().addOnSuccessListener { snapshot ->
            val ssid = snapshot.value?.toString()

            if (ssid.isNullOrEmpty()){
                //ssid missing -> force configuration
                startSettingsActivity()
            }else{
                //ssid exists -> go home
                //also save ssid locally so next launch is instant
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("LOCAL_SSID", ssid)
                    .apply()
                startMainActivity()
            }
        }.addOnFailureListener { startMainActivity() }
    }

    //move to MainActivity
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // close LoginActivity
        overridePendingTransition(0,0)
    }

    private fun startSettingsActivity(){
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }




}