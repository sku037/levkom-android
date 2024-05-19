package com.example.levkomandroid.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.levkomandroid.R
import com.example.levkomandroid.databinding.FragmentEditRouteBinding

class EditRouteFragment : Fragment() {
    protected lateinit var binding: FragmentEditRouteBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditRouteBinding.inflate(inflater, container, false)
        return binding.root
    }
}