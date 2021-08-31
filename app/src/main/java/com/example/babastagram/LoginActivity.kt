package com.example.babastagram

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    var TAG : String? = "로그 LoginActivity - "
    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    private var GOOGLE_LOGIN_CODE = 9001
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        btn_email_login.setOnClickListener {
            signInAndSignup()
        }

        btn_google_login.setOnClickListener{
            googleLogin()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("633455741660-c84ufoljj231eo2hdt4see5hu3sfa5rl.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)
    }

    fun googleLogin(){
        Log.d(TAG, "googleLogin()")
        val signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult()")
        if(requestCode == GOOGLE_LOGIN_CODE) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if(result!!.isSuccess){
                Log.d(TAG,"onActivityResult 01 result!!.isSuccess")
                val account = result.signInAccount
                //Second step
                firebaseAuthWithGoogle(account)

            }
        }
    }

    private fun firebaseAuthWithGoogle(account : GoogleSignInAccount? ) {
        Log.d(TAG, "firebaseAuthWithGoogle()")
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG,"firebaseAuthWithGoogle 01 task.isSuccessful")
                    //Login
                    moveMainPage(task.result?.user)

                } else {
                    //show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()

                }
            }
    }

    //자동로그인기능
    override fun onStart() {
        Log.d(TAG, "onStart()")
        super.onStart()
        moveMainPage(auth?.currentUser)
    }

    private fun signInAndSignup(){
        Log.d(TAG, "signInAndSignup()")
        auth?.createUserWithEmailAndPassword(edit_email.text.toString(), edit_password.text.toString())
            ?.addOnCompleteListener {
                task ->
                if (task.isSuccessful){
                    //Creating a user account
                    moveMainPage(task.result?.user)
                }else {
                    //Login if you have account
                    signInEmail()
                }
            }
    }

    private fun signInEmail() {
        Log.d(TAG, "signInEmail()")
        auth?.signInWithEmailAndPassword(edit_email.text.toString(), edit_password.text.toString())
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Login
                    moveMainPage(task.result?.user)
                } else {
                    //show the error message
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()

                }
            }

    }

    private fun moveMainPage(user: FirebaseUser?) {
        if (user != null) {
            Log.d(TAG, "moveMainPage()")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }


}