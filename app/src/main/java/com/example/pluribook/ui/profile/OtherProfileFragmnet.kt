package com.example.pluribook.ui.profile

import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

class OtherProfileFragment : ProfileFragment() {

    private val args: OtherProfileFragmentArgs by navArgs()

    override fun getTargetUserId(): String = args.targetUserId

    override fun navigateToPostDetail(postId: String) {
        val action = OtherProfileFragmentDirections.actionOtherProfileToPostDetail(postId)
        findNavController().navigate(action)
    }
}