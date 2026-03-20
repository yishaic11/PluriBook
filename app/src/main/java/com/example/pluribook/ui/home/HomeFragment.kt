package com.example.pluribook.ui.home

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.pluribook.NavGraphDirections
import com.example.pluribook.PluribookApplication
import com.example.pluribook.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var homeAdapter: HomeAdapter
    private var loadedCount = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_home)
        val centralProgress = view.findViewById<ProgressBar>(R.id.progress_bar_home)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_home)
        val buttonLogout = view.findViewById<MaterialButton>(R.id.button_logout_home)

        buttonLogout.setOnClickListener {
            val database = (requireActivity().application as PluribookApplication).database
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }
                FirebaseAuth.getInstance().signOut()

                val action = NavGraphDirections.actionGlobalLoginFragment()
                findNavController().navigate(action)
            }
        }

        homeAdapter = HomeAdapter(
            onImageLoaded = {
                loadedCount++
                if (loadedCount >= 6 || loadedCount >= homeAdapter.itemCount) {
                    centralProgress.visibility = View.GONE
                }
            },
            onPostClick = { clickedPost ->
                viewModel.savePostToLocal(clickedPost)

                val action = HomeFragmentDirections.actionHomeFragmentToPostFragment(clickedPost.id)
                findNavController().navigate(action)
            }
        )

        setFragmentResultListener("post_request") { _, bundle ->
            val postWasDeleted = bundle.getBoolean("post_deleted", false)
            if (postWasDeleted) {
                viewModel.refreshPosts()
            }
        }

        recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = homeAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.postsFlow.collectLatest { pagingData ->
                loadedCount = 0
                homeAdapter.submitData(pagingData)
            }
        }

        homeAdapter.addLoadStateListener { loadState ->
            val isInitialLoading = loadState.source.refresh is LoadState.Loading
            val isListEmpty = homeAdapter.itemCount == 0

            centralProgress.visibility =
                if (isInitialLoading && isListEmpty) View.VISIBLE else View.GONE
            swipeRefresh.isRefreshing = isInitialLoading && !isListEmpty


            if (loadState.source.append.endOfPaginationReached && !isListEmpty) {
                viewModel.loadMorePosts()
            }
        }

        swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }
    }
}