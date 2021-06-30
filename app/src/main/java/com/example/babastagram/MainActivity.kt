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
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
//        var detailViewFragment = DetailViewFragment()
//        supportFragmentManager.beginTransaction().replace(R.id.main_content, detailViewFragment).commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d(TAG,"onNavigationItemSelected 01")

        setToolbarDefault()

        when(item.itemId){
            R.id.it_home ->{
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, detailViewFragment).commit()
                return true
            }
            R.id.it_search ->{
                var gridFragment = GridFragment()
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
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, alarmFragment).commit()
                return true
            }
            R.id.it_account ->{

                Log.d(TAG, "onNavigationItemSelected R.id.it_account 01")
                var userFragment = UserFragment()
                var bundle = Bundle()
                var uid = FirebaseAuth.getInstance().currentUser?.uid

                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle
                Log.d(TAG, "onNavigationItemSelected R.id.it_account 02")

                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment).commit()
                return true


                Log.d(TAG,"onNavigationItemSelected it_account")
//                var userFragment = UserFragment()
                supportFragmentManager.beginTransaction().replace(R.id.main_content, userFragment).commit()
                return true
            }
        }
        return false
    }

    fun setToolbarDefault(){
        main_user.visibility = View.GONE
        main_img_back.visibility = View.GONE
        main_img_logo_title.visibility = View.VISIBLE

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