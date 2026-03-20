package com.example.pluribook.ui.comment

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pluribook.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommentFragment : Fragment(R.layout.fragment_comment) {

    private val viewModel: CommentViewModel by viewModels()
    private lateinit var adapter: CommentAdapter
    private val args: CommentFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = args.postId

        val buttonBack = view.findViewById<ImageButton>(R.id.image_button_back_comments)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_bar_comments)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_comments)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout_comment)
        val editText = view.findViewById<TextInputEditText>(R.id.edit_text_comment)

        buttonBack.setOnClickListener { findNavController().navigateUp() }

        adapter = CommentAdapter(viewModel.currentUserId) { comment ->
            AlertDialog.Builder(requireContext())
                .setTitle("Comment Options")
                .setItems(arrayOf("Delete Comment")) { _, which ->
                    if (which == 0) {
                        viewModel.deleteComment(postId, comment.id)
                    }
                }.show()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getCommentsFlow(postId).collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }

        adapter.addLoadStateListener { loadState ->
            val isLoading = loadState.source.refresh is LoadState.Loading
            if (isLoading && adapter.itemCount == 0) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        }

        inputLayout.setEndIconOnClickListener {
            val text = editText.text.toString()
            if (text.isNotBlank()) {
                viewModel.addComment(postId, text) {
                    recyclerView.scrollToPosition(adapter.itemCount)
                }
                editText.text?.clear()
            }
        }
    }
}