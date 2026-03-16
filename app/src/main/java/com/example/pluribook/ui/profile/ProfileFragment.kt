package com.example.pluribook.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.pluribook.R
import com.example.pluribook.utils.ResourceState
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var adapter: ProfilePostAdapter

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var targetProfileId: String = currentUserId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textUsername = view.findViewById<TextView>(R.id.text_username)
        val imageProfile =
            view.findViewById<ShapeableImageView>(R.id.image_profile_picture)
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btn_edit_profile)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout_profile)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_posts)

        adapter = ProfilePostAdapter(emptyList()) { clickedPost ->
            val bundle = Bundle().apply {
                putString("postId", clickedPost.id)
            }
            findNavController().navigate(R.id.post_fragment, bundle)
        }
        recyclerView.adapter = adapter

        val isCurrentUser = (targetProfileId == currentUserId)

        if (!isCurrentUser) {
            btnEditProfile.visibility = View.GONE
            tabLayout.visibility = View.GONE
        } else {
            btnEditProfile.setOnClickListener {
                Toast.makeText(requireContext(), "Edit Profile coming soon!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> profileViewModel.loadUserPosts(targetProfileId)
                    1 -> profileViewModel.loadLikedPosts(targetProfileId)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> profileViewModel.loadUserPosts(targetProfileId)
                    1 -> profileViewModel.loadLikedPosts(targetProfileId)
                }
            }
        })

        profileViewModel.userProfileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Success -> {
                    val user = state.data
                    textUsername.text = user.username

                    if (user.photoUrl.isNotEmpty()) {
                        Picasso.get()
                            .load(user.photoUrl)
                            .placeholder(R.drawable.create_post_nav_icon)
                            .fit()
                            .centerCrop()
                            .into(imageProfile)
                    }
                }

                is ResourceState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                is ResourceState.Loading -> {
                    // TODO: set a loading state for the text
                }
            }
        }

        profileViewModel.profilePostsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ResourceState.Loading -> {
                    // Handled implicitly or you can show a ProgressBar
                    // TODO: Loading spinner
                }

                is ResourceState.Success -> {
                    adapter.updatePosts(state.data)
                }

                is ResourceState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        profileViewModel.loadUserPosts(targetProfileId)
        profileViewModel.loadUserProfile(targetProfileId)
    }
}