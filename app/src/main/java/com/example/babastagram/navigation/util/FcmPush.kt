package com.example.babastagram.navigation.util

import android.util.Log
import com.example.babastagram.navigation.menu.PushDTO
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class FcmPush {
    var TAG : String? = "로그 FcmPush - "
    //    var JSON=  MediaType.parse("application/json; charset=utf-8")
    var JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverKey = "AAAAk3znitw:APA91bFNURy1fNV8pvvjQpdiegwYXru0ywAWIwsSIGOG4MxN-OKr8kzFdRYQhoMgHJqSCDw0NzQ9HiUgj5YtnKZ3HmhKHxf7DJ7okisCZhhFSSpMXk4FacuPQfNKIWP7M6w5iInHbKLI"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null

    companion object {
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid : String, title : String, message : String){
        Log.d(TAG,"sendMessage 01 destinationUid = " +destinationUid.toString() +"   title = "+ title  +"   message = "+ message)
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener {
            task ->
            if(task.isSuccessful){
                val token = task?.result?.get("pushToken").toString()

                val pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title =title
                pushDTO.notification.body = message

                val body = gson?.toJson(pushDTO)!!.toRequestBody(JSON)
                val request = Request.Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=$serverKey")
                    .url(url)
                    .post(body)
                    .build()

                Log.d(TAG, "request = $request")
                okHttpClient?.newCall(request)?.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.d(TAG, "onFailure 01 call = $call   exception = $e")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.d(TAG, "onResponse 01 call = $call   response = $response")
                        println(response.body?.string())
                    }

                })

//                okHttpClient?.newCall(request)?.enqueue(object : Callback{
//                    override fun onFailure(call: Call, e: IOException) {
//                        Log.d(TAG,"onFailure 01 call = " +call.toString() +"   exception = "+ e.toString())
//                    }
//
//                    override fun onResponse(call: Call, response: Response) {
//                        Log.d(TAG,"onResponse 01 call = " +call.toString() +"   response = "+ response.toString())
//                        println(response.body?.string())
//                    }
//                })
            }
        }
    }

}