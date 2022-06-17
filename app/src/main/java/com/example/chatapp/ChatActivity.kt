package com.example.chatapp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.chat_row.view.*
import kotlinx.android.synthetic.main.chat_row_me.view.*


class ChatActivity : AppCompatActivity() {

    val adapter = GroupAdapter<ViewHolder>()
    //Count used for counting messages (So that the location knows where to scroll to)
    var count = 0
    //Other users UID will be stored here
    var OtherUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //Disable night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        recyclerView_chat.adapter = adapter

        //Set OtherUid to the other users UID and set top bar name to other persons username
        val user = intent.getParcelableExtra<User>(MessageActivity.USERKEY)
        if (user != null) {
            supportActionBar?.title = user.username
            OtherUid = user.uid
        }

        //Add messages
        listenForMessages()

        //When the send button is clicked, send the message and clear the chat bar. Afterwards,
        //update the scroll to the current message that was just sent.
       send_button.setOnClickListener(){
           if(message_bar.text.isEmpty()){
               return@setOnClickListener
           }

           sendMessage()
           message_bar.text.clear()
           recyclerView_chat.smoothScrollToPosition(count)


           //Hides softkeyboard
           val imm: InputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
           if (imm.isActive)
               imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
       }



    }




    private fun listenForMessages(){
        val ref = FirebaseDatabase.getInstance().getReference("/messages")
        ref.addChildEventListener(object: ChildEventListener {
            val otherUser = intent.getParcelableExtra<User>(MessageActivity.USERKEY)
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)

                //Add the chat message derived from the snapshot into the recyclerview chat
                //Decide which instance of ChatItem you wanna use based on from and to IDs
                if(chatMessage != null && otherUser != null){
                    if(chatMessage.fromId == FirebaseAuth.getInstance().uid && chatMessage.toId == otherUser.uid){
                        adapter.add(ChatItemMe(chatMessage.text))
                        count++
                    }
                    else if(chatMessage.toId == FirebaseAuth.getInstance().uid && chatMessage.fromId == otherUser.uid){
                        adapter.add(ChatItem(chatMessage.text))
                        count++
                    }
                }
                //Scroll to new position in chat
                recyclerView_chat.smoothScrollToPosition(count)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }
        })
    }

    //Create instance of chatmessage and push it to database
    private fun sendMessage(){
        val ref = FirebaseDatabase.getInstance().getReference("/messages").push()

        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(MessageActivity.USERKEY)
        val toId = user?.uid

        val text = message_bar.text.toString()
        val chatMessage = ChatMessage(ref.key!!, text, fromId!!,toId!!, System.currentTimeMillis() / 1000)
        ref.setValue(chatMessage)
        count++
    }
}

//Chat message object with all resources
class ChatMessage(val id: String, val text: String, val fromId: String, val toId: String, val timestamp: Long){
    constructor() : this("", "", "", "", -1)
}

//ChatItem for other persons message
class ChatItem(val text: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.message_chat.text = text
    }
    override fun getLayout(): Int {
        return R.layout.chat_row
    }
}

//ChatItem for my messages
class ChatItemMe(val text: String): Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.message_chat_me.text = text
    }
    override fun getLayout(): Int {
        return R.layout.chat_row_me
    }
}