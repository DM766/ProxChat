package com.example.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_main.*

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //Checks for button input
        register_button.setOnClickListener {
            //Takes input from email and password text fields
            val email = email_input.text.toString()
            val password = password_input.text.toString()

            //If the text fields are empty, just return. This will avoid the app crashing.
            //Also inform the user to enter something
           if(email.isEmpty() || password.isEmpty()){
               Toast.makeText(this, "Please enter both an email and password.", Toast.LENGTH_SHORT).show()
               return@setOnClickListener
            }

            //Firebase Authentication Attempt, signing in with email and password
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener{
                   if(!it.isSuccessful) return@addOnCompleteListener
                    else{
                       UploadUser()
                   }

            }
                .addOnFailureListener{
                    Toast.makeText(this,"An error has occured. Please try again.", Toast.LENGTH_SHORT).show()
                    return@addOnFailureListener
                }

        }

        login_text.setOnClickListener {
            //Enter the login screen when tapped
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }



    }
    private fun UploadUser(){

        //Get instance of uid and save it into uid
        val uid = FirebaseAuth.getInstance().uid

        //Check case of uid is null
        if(uid == null) return

        //Set ref to location in database where users are stored. The user will be marked with uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        //Create hashmap object of user, mapping uid to uid and name to username_input
       // val user = hashMapOf("uid" to uid, "name" to username_input.text.toString())
        val user = User(uid, username_input.text.toString(), 0.00, 0.00, true)


        //Add to database
        ref.setValue(user)
            .addOnSuccessListener {
                val intent = Intent(this, MessageActivity::class.java)
                //Close all activties for new task
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addOnFailureListener{
                Toast.makeText(this,"An error has occured. Please try again.", Toast.LENGTH_SHORT).show()
                return@addOnFailureListener
            }
    }

}

//User object which holds all the relevant data of a user
@Parcelize
class User(val uid: String, val username: String, var lat: Double, var lon: Double, var online: Boolean) : Parcelable{
    constructor() : this("","", 0.00, 0.00, false)
}

