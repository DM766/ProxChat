package com.example.chatapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_message.*
import kotlinx.android.synthetic.main.user_row.view.*

//This switch makes it so that the location update which occurs every second, only gets started once.
//Otherwise, it would keep stacking and make additional unnecessary instances of it
public var switch = true

//Variables used for keeping track of our latitude and longitude
public var lat = 0.00
public var lon = 0.00

//Used to differentiate when we're online and when we're not
public var online = true

class MessageActivity : AppCompatActivity(), LifecycleObserver {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        //Set title of top bar
        supportActionBar?.title = "Users near me"

        //Disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //Dependency needed for location services
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        //When entering this activity, it implies you've already registered or logged in, so the user
        //can be set to online in the database
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        val myUid = FirebaseAuth.getInstance().uid
        if(myUid != null) {
            ref.child(myUid).child("online").setValue(true)
        }


        //Check if the user is already registered and isn't here by mistake
        Verify()

        //Update location of user
        updateLocation()

        //Create vertical bars to separate users in recyclerview
        recyclerview.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        //Add users to the recycler view
        addUsers()

        //Every second, this block of code runs and calls the functions updateLocation and addUsers
        //It updates our location and additionally updates the recyclerview
        if(switch == true) {
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post(object : Runnable {
                override fun run() {
                        updateLocation()
                        addUsers()
                    mainHandler.postDelayed(this, 1000)
                }
            })
        }

        //Setting switch to false so that we don't stack extra instances of the code block above
        switch = false

        //Dependency used to track status of application
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    //Block of code runs when its detected that the app stops. It sets our online status to false.
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded(){
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        val myUid = FirebaseAuth.getInstance().uid

        if(myUid != null) {
            ref.child(myUid).child("online").setValue(false)
        }
        online = false
    }

    //Block of code runs when its detected that the app is destroyed. It sets our online status to false.
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppDestroyed(){
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        val myUid = FirebaseAuth.getInstance().uid

        if(myUid != null) {
            ref.child(myUid).child("online").setValue(false)
        }
        online = false
    }

    //Block of code runs when its detected that the app resumes. It sets our online status to true.
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onAppResumed(){
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        val myUid = FirebaseAuth.getInstance().uid

        if(myUid != null) {
            ref.child(myUid).child("online").setValue(true)
        }
        online = true
    }

    //Useful variables for other functions stored in a companion object
    companion object{
        val USERKEY = "USER_KEY"
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    //Grabs the route to our "lat" and "lon" variables in the database and updates them with
    //our current latitude and longitude
    private fun updateLocationInDatabase(){
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        val myUid = FirebaseAuth.getInstance().uid
        if(myUid != null){
            ref.child(myUid).child("lat").setValue(lat)
            ref.child(myUid).child("lon").setValue(lon)
        }
    }

    //Add users to recyclerview
    private fun addUsers(){
        //Route to users in database
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        //Access database
        ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                val adapter = GroupAdapter<ViewHolder>()

                val myUid = FirebaseAuth.getInstance().uid

                //For each node, do the following:
                p0.children.forEach {
                    //Get a snapshot of the current node and extract the latitude and longitude nodes
                    val user = it.getValue(User::class.java)
                    val themLat = user?.lat
                    val themLon = user?.lon

                    //If they are set to online AND they are within a certain range, add them to my recyclerview
                    if (user != null && themLat != null && themLon != null && user.uid != myUid && user.online == true) {
                        val loc1 : Location = Location(LocationManager.GPS_PROVIDER)
                        loc1.latitude = lat
                        loc1.longitude = lon

                        val loc2 : Location = Location(LocationManager.GPS_PROVIDER)
                        loc2.latitude = user.lat
                        loc2.longitude = user.lon

                        //This is roughly 2 miles in meters
                        if(loc1.distanceTo(loc2) <= 3218.0) {
                            adapter.add(UserItem(user))
                        }
                    }
                }

                //When a user is clicked on from the recyclerview, start chat activity with them
                //passed through intent
                adapter.setOnItemClickListener { item, view ->
                    val userItem = item as UserItem

                    val intent = Intent(view.context, ChatActivity::class.java)
                    intent.putExtra(USERKEY, userItem.user)
                    startActivity(intent)
                }

                recyclerview.adapter = adapter
            }

            override fun onCancelled(p0: DatabaseError) {
                return
            }
        })
    }

    private fun Verify(){
        //Check if the user is signed in. If they are not, send them to register page
        val uid = FirebaseAuth.getInstance().uid
        if(uid == null){
            val intent = Intent(this, RegisterActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            //When clicking sign out, set my status to false, sign me out from authentication, send me back to register activity
            R.id.menu_sign_out -> {
                val ref = FirebaseDatabase.getInstance().getReference("/users")
                val myUid = FirebaseAuth.getInstance().uid

                if(myUid != null) {
                    ref.child(myUid).child("online").setValue(false)
                }

                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }


        }

        //Manual refresh button (Just to act as an emergency feed)
        when(item?.itemId){
            R.id.menu_refresh -> {
                addUsers()
            }
        }


        return true
    }

    //Method which instantiates menu and creates sign out button
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //This function makes sure every condition is met to updatelocation and stores result in lat and lon
    private fun updateLocation(){
        if(checkPermissions()){
            if(isLocationEnabled()){

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){ task ->
                    val location: Location?=task.result
                    if(location == null && online == true){
                        Toast.makeText(this, "Please turn on your location", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        if (location != null) {
                            lat = location.latitude
                        }
                        if (location != null) {
                            lon = location.longitude
                        }
                        updateLocationInDatabase()
                    }
                }
            }
            else{
                //setting open here
                Toast.makeText(this, "Please turn on your location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            //request perm
            requestPermission()
        }
    }


//Checks if location is enabled
    private fun isLocationEnabled() : Boolean{
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    //Requests permissions
    private fun requestPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION),
        PERMISSION_REQUEST_ACCESS_LOCATION)
    }

    //Check status of current permissions
    private fun checkPermissions(): Boolean{
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            return true
        }
        return false
    }

    //Process the aftermath of requesting permissions from user
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_REQUEST_ACCESS_LOCATION){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                updateLocation()
            }
            else{
                if(switch == true) {
                    Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}

//Source of each individual item in the recyclerview
class UserItem(val user: User): Item<ViewHolder>(){

    override fun bind(viewHolder: ViewHolder, position: Int){
        viewHolder.itemView.user.text = user.username
    }
    override fun getLayout(): Int {
        return R.layout.user_row
    }
}