package com.example.pluribook.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pluribook.R
import com.example.pluribook.utils.ResourceState
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class ProfileFragment : Fragment(R.layout.fragment_profile) {

    protected val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var adapter: ProfilePostAdapter

    protected val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var pagingJob: Job? = null
    private var loadedCount = 0

    protected open fun getTargetUserId(): String? = arguments?.getString("targetUserId")
    protected open fun navigateToPostDetail(postId: String) {
        val action = ProfileFragmentDirections.actionProfileFragmentToPostDetailFragment(postId)
        findNavController().navigate(action)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetProfileId = getTargetUserId() ?: currentUserId

        val textUsername = view.findViewById<TextView>(R.id.text_username)
        val imageProfile = view.findViewById<ShapeableImageView>(R.id.image_profile_picture)
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btn_edit_profile)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout_profile)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_posts)
        val centralProgress = view.findViewById<ProgressBar>(R.id.progress_bar_profile)
        val buttonBack = view.findViewById<ImageButton>(R.id.button_back_profile)

        adapter = ProfilePostAdapter(
            onImageLoaded = {
                loadedCount++
                if (loadedCount >= 6 || loadedCount >= adapter.itemCount) {
                    centralProgress.visibility = View.GONE
                }
            },
            onPostClick = { clickedPost ->
                navigateToPostDetail(clickedPost.id)
            }
        )

        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            this.adapter = this@ProfileFragment.adapter
        }

        adapter.addLoadStateListener { loadState ->
            val isInitialLoading = loadState.source.refresh is LoadState.Loading
            val isListEmpty = adapter.itemCount == 0

            centralProgress.visibility =
                if (isInitialLoading && isListEmpty) View.VISIBLE else View.GONE
        }

        val isCurrentUser = (targetProfileId == currentUserId)

        if (!isCurrentUser) {
            buttonBack.visibility = View.VISIBLE
            btnEditProfile.visibility = View.GONE
            tabLayout.visibility = View.GONE
        } else {
            buttonBack.visibility = View.GONE
            btnEditProfile.setOnClickListener {
                val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment()
                findNavController().navigate(action)
            }
        }

        buttonBack.setOnClickListener { findNavController().navigateUp() }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> observeUserPosts(targetProfileId)
                    1 -> observeLikedPosts(targetProfileId)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        profileViewModel.userProfileState.observe(viewLifecycleOwner) { state ->
            if (state is ResourceState.Success) {
                val user = state.data
                textUsername.text = user.username
                if (user.photoUrl.isNotEmpty()) {
                    Picasso.get().load(user.photoUrl).placeholder(R.drawable.default_profile_photo)
                        .fit().centerCrop().into(imageProfile)
                }
            }
        }

        profileViewModel.likedPostIds.observe(viewLifecycleOwner) { ids ->
            if (tabLayout.selectedTabPosition == 1) {
                pagingJob?.cancel()
                pagingJob = viewLifecycleOwner.lifecycleScope.launch {
                    profileViewModel.getLikedPostsFlow(ids).collectLatest { pagingData ->
                        loadedCount = 0
                        adapter.submitData(pagingData)
                    }
                }
            }
        }

        profileViewModel.loadUserProfile(targetProfileId)

        observeUserPosts(targetProfileId)
    }

    private fun observeUserPosts(targetId: String) {
        pagingJob?.cancel()
        pagingJob = viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.getPostsFlow(targetId).collectLatest { pagingData ->
                loadedCount = 0
                adapter.submitData(pagingData)
            }
        }
        profileViewModel.refreshUserPosts(targetId)
    }

    private fun observeLikedPosts(targetId: String) {
        profileViewModel.refreshLikedPosts(targetId)
    }
}