package com.example.chatapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        //Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //Listening for login button to be tapped
        login_button.setOnClickListener{
            //Take email/password text fields from user input
            val email = email_inputL.text.toString()
            val password = password_inputL.text.toString()

            //If they are empty, inform the user to enter them
            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Please enter both an email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Firebase auth sign in
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(){
                    if(!it.isSuccessful) return@addOnCompleteListener
                    else{
                        val intent = Intent(this, MessageActivity::class.java)
                        //Close all activties for new task
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
                .addOnFailureListener(){
                    Toast.makeText(this,"An error has occured. Please try again.", Toast.LENGTH_SHORT).show()
                    return@addOnFailureListener
                }

        }


    }


}