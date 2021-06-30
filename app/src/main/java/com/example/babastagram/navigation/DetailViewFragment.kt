package com.example.babastagram.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.babastagram.R
import com.example.babastagram.navigation.menu.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {
    var firestore : FirebaseFirestore? = null
    var uid : String? = null
    var TAG : String? = "로그 DetailViewFragment - "

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail,container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        Log.d(TAG,"onCreate 01 uid = "+ uid.toString())


        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()

        init {
            Log.d(TAG,"DetailViewRecyclerViewAdapter 01")
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener{querySnapshot, firebaseFiresotrException ->
                contentDTOs.clear()
                contentUidList.clear()
                if(querySnapshot == null) return@addSnapshotListener

                for (snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                notifyDataSetChanged()
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            Log.d(TAG,"onCreateViewHolder 01")
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail,parent,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            Log.d(TAG,"onBindViewHolder 01")
            var viewholder = (holder as CustomViewHolder).itemView

            //UserId
            viewholder.detailviewitem_profile_textview.text = contentDTOs!![position].userId

            //Image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewholder.detailView_image_content)

            //Explain of content
            viewholder.detailviewitem_explain_textview.text = contentDTOs!![position].explain

            //likes
            viewholder.detailView_favoritecounter_textview.text ="Likes "+ contentDTOs!![position].favoriteCount

            //This code is when the button is clicked
            viewholder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }

            //This code is when the page is loaded
            if (contentDTOs!![position].favorites.containsKey(uid)){
                //This is like status
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            }else {
                //This is unlike status
                viewholder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            //This code is when the profile image is clicked
            viewholder.detailViewItem_profile_image.setOnClickListener {
                Log.d(TAG,"onBindViewHolder 01  detailViewItem_profile_image 01")
                var fragment = UserFragment()
                var bundle = Bundle()

                Log.d(TAG,"onBindViewHolder 02  contentDTOs[position].uid = " + contentDTOs[position].uid.toString())
                Log.d(TAG,"onBindViewHolder 03  contentDTOs[position].userId = " +  contentDTOs[position].userId.toString())

                bundle.putString("destinationUid" , contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle

                activity?.supportFragmentManager?.beginTransaction()?.replace(R.id.main_content, fragment)?.commit()
            }

            viewholder.detailViewItem_comment_imageview.setOnClickListener { v ->
                var intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                startActivity(intent)
            }

        }

        override fun getItemCount(): Int {
            Log.d(TAG,"getItemCount 01")
            return contentDTOs.size
        }

        fun favoriteEvent(position: Int){
            Log.d(TAG,"favoriteEvent 01")
            var tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transition ->

                var contentDTO = transition.get(tsDoc!!).toObject(ContentDTO::class.java)

                if (contentDTO!!.favorites.containsKey(uid)){
                    //When the button is clicked
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount -1
                    contentDTO?.favorites.remove(uid)
                }else {
                    //When the button is not clicked
                    contentDTO?.favoriteCount = contentDTO?.favoriteCount +1
                    contentDTO?.favorites[uid!!] = true
                }
                transition.set(tsDoc, contentDTO)
            }

        }

    }
}