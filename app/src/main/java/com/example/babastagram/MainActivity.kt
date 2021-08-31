package com.example.babastagram

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.babastagram.R
import com.example.babastagram.navigation.*
import com.example.babastagram.navigation.util.FcmPush
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() , BottomNavigationView.OnNavigationItemSelectedListener{
    var TAG : String? = "로그 MainActivity - "

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate 01")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottom_menu.setOnNavigationItemSelectedListener(this)
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1)

        //Set default screen
        bottom_menu.selectedItemId = R.id.it_home
        registerPushToken()
//        var detailViewFragment = DetailViewFragment()
//        supportFragmentManager.beginTransaction().replace(R.id.main_content, detailViewFragment).commit()
    }


//    override fun onStop() {
//        super.onStop()
//        FcmPush.instance.sendMessage("IRKuwCeSIiWqZTe71aoXpqg4fWD3","babastagrm","Yo!")
//    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d(TAG,"onNavigationItemSelected 01")

        setToolbarDefault()

        when(item.itemId){
            R.id.it_home ->{
                val detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, detailViewFragment).commit()
                return true
            }
            R.id.it_search ->{
                val gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, gridFragment).commit()
                return true
            }
            R.id.it_photo ->{

                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    startActivity(Intent(this, AddPhotoActivity::class.java))
                }

//                var addPhotoActivity = AddPhotoActivity()
//                supportFragmentManager.beginTransaction().replace(R.id.main_content, addPhotoActivity).commit()
                return true
            }
            R.id.it_alarm ->{
                val alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment).commit()
                return true
            }
            R.id.it_account ->{

                Log.d(TAG, "onNavigationItemSelected R.id.it_account 01")
                val userFragment = UserFragment()
                val bundle = Bundle()
                val uid = FirebaseAuth.getInstance().currentUser?.uid

                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                Log.d(TAG, "onNavigationItemSelected R.id.it_account 02")

                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment).commit()
                return true
            }
        }
        return false
    }

    private fun setToolbarDefault(){
        main_user.visibility = View.GONE
        main_img_back.visibility = View.GONE
        main_img_logo_title.visibility = View.VISIBLE

    }

    private fun registerPushToken(){
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            val token = task.result
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val map = mutableMapOf<String, Any>()
            map["pushToken"] = token!!

            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
        })
//        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener{
//            task ->
//            var token = task.result?.token
//            val uid = FirebaseAuth.getInstance().currentUser?.uid
//            var map = mutableMapOf<String, Any>()
//            map["pushToken"] = token!!
//
//            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
//        }


            // Get new FCM registration token
//            val token = task.result



            // Log and toast
//            val msg = getString(R.string., token)
//            Log.d(TAG, msg)
//            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if( requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK){
            var imageUri = data?.data
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            var storageRef = FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
            storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                var map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }

        }
    }

}