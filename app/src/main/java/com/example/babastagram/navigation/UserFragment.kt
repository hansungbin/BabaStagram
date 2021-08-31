package com.example.babastagram.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.babastagram.LoginActivity
import com.example.babastagram.MainActivity
import com.example.babastagram.R
import com.example.babastagram.navigation.menu.AlarmDTO
import com.example.babastagram.navigation.menu.ContentDTO
import com.example.babastagram.navigation.menu.FollowDTO
import com.example.babastagram.navigation.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    var TAG: String? = "로그 UserFragment.kt -- "
    var fragmentView: View? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var currentUserUid : String? = null

    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView 01")
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid =auth?.currentUser?.uid

//        Log.d(TAG, "onCreateView fragmentView = " + fragmentView.toString())
//        Log.d(TAG, "onCreateView uid = " + uid.toString())
//        Log.d(TAG, "onCreateView firestore = " + firestore.toString())
//        Log.d(TAG, "onCreateView auth = " + auth.toString())
//        Log.d(TAG,"onCreateView 02 uid = $uid + currentUserUid = $currentUserUid")

        val unit = if (uid == currentUserUid) {
            //MyPage
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }

        } else {
            //OtherUserPage
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)

            val mainActivity = (activity as MainActivity)
            mainActivity.main_user?.text = arguments?.getString("userId")
            mainActivity.main_img_back?.setOnClickListener {
                mainActivity.bottom_menu.selectedItemId = R.id.it_home
            }

            mainActivity.main_img_logo_title?.visibility = View.GONE
            mainActivity.main_user?.visibility = View.VISIBLE
            mainActivity.main_img_back?.visibility = View.VISIBLE

            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }


        fragmentView?.account_recyclerview?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerview?.layoutManager = GridLayoutManager(requireActivity(), 3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)

        }

        Log.d(TAG, "onCreateView 11")

        getProfileImages()
        getFollowerAndFollowing()
        return fragmentView
    }

    private fun requestFollow(){
        //Save data to my account
        val tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->

            // followings : 팔로잉 하고 있는 사람 목록
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            Log.d(TAG, "requestFollow() -  followDTO.toString() =" + followDTO.toString())


            // 팔로잉한 사람이 아무도 없을때 following 카운트에 한명을 늘리고 자신을 follower에 넣는다.
            if (followDTO == null) {
                Log.d(TAG, "requestFollow() 01 (followDTO == null)-  followDTO =" + followDTO.toString())
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                // 상대방을 넣는다.
                followDTO.followings[uid!!] = true
                followerAlarm(uid!!)
                Log.d(TAG, "requestFollow() 02 (followDTO == null)-  followDTO =$followDTO")
                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            // followDTO 안에 값이 있을 경우 들어온 uid 와 비교를 한다.
            // 내가 follow를 한 상태에서 following 취소
            if (followDTO.followings.containsKey(uid)){
                Log.d(TAG,
                    "requestFollow() 03  (followDTO.followings.containsKey(uid))-  followDTO =$followDTO"
                )
                //It remove following third person when a third person follow me
                followDTO.followingCount = followDTO.followingCount -1
                followDTO.followings.remove(uid)
            }else {
                // following 을 하면 됨
                Log.d(TAG,
                    "requestFollow() 04 else (followDTO.followings.containsKey(uid))-  followDTO =$followDTO"
                )
                //It add following  third person when a third person do not follow me
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }

        //Save data to third person 상대방 계정에 보이는 화면
        val tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if(followDTO!!.followers.containsKey(currentUserUid)){
                //It cancel my follower when I follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            }else {
                //It add my follower when I don't follow a third person
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    @SuppressLint("UseRequireInsteadOfGet")
    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            val followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if (followDTO?.followingCount != null) {
                fragmentView?.account_tv_following_count?.text = followDTO.followingCount.toString()
            }
            if (followDTO?.followerCount != null){
                fragmentView?.account_tv_follower_count?.text = followDTO.followerCount.toString()
                if (followDTO.followers.containsKey(currentUserUid!!)){
                    fragmentView?.account_btn_follow_signout?.text =  getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!,R.color.colorLightGray),PorterDuff.Mode.MULTIPLY)
                }else {
                    if (uid != currentUserUid){
                        fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                        followerAlarm(uid!!)
                    }
                }
            }
        }
    }

    private fun followerAlarm(destinationUid : String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userID = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        val message = auth?.currentUser?.email + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "baba_stargram", message)
    }
    private fun getProfileImages(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){
                val url = documentSnapshot.data!!["image"]
                Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            Log.d(TAG, "UserFragmentRecyclerViewAdapter 01")
            firestore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    //Sometimes, This code return null of querySnapshot when it signout
                    if (querySnapshot == null) return@addSnapshotListener
                    Log.d(TAG, "UserFragmentRecyclerViewAdapter 02")
                    //Get data
                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }

                    fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()

                    fragmentView?.account?.visibility = View.VISIBLE
                    fragmentView?.toolbar_division?.visibility = View.VISIBLE //

                    Log.d(TAG, "UserFragmentRecyclerViewAdapter 03")
                    notifyDataSetChanged()
                    Log.d(TAG, "UserFragmentRecyclerViewAdapter 04")
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            Log.d(TAG, "onCreateViewHolder 01")
            val width = resources.displayMetrics.widthPixels / 3
            val imageview = ImageView(parent.context)
            Log.d(TAG, "onCreateViewHolder 02")
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageview)
        }

        inner class CustomViewHolder(var imageview: ImageView) :
            RecyclerView.ViewHolder(imageview) {

        }

        override fun getItemCount(): Int {
            Log.d(TAG, "getItemCount 01")
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            Log.d(TAG, "onBindViewHolder 01")
            val imageview = (holder as CustomViewHolder).imageview
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageview)
        }
    }

}