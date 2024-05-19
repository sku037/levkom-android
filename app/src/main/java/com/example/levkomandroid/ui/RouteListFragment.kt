package com.example.levkomandroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.levkomandroid.LevkomApplication
import com.example.levkomandroid.R
import com.example.levkomandroid.databinding.FragmentRouteListBinding
import com.example.levkomandroid.viewmodel.LevkomViewModel
import com.example.levkomandroid.viewmodel.LevkomViewModelFactory

class RouteListFragment : Fragment() {

    private lateinit var binding: FragmentRouteListBinding
    private val viewModel: LevkomViewModel by activityViewModels {
        LevkomViewModelFactory(
            (activity?.application as LevkomApplication).levkomRepository
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRouteListBinding.inflate(inflater, container, false)
        val adapter = RouteAdapter()
        binding.rvRouteList.adapter = adapter
        viewModel.routes.observe(viewLifecycleOwner) { routes ->
            adapter.submitList(routes)
        }
        setupSwipeListener(binding.rvRouteList)
        setupClickListener(adapter)
        return binding.root
    }

    private fun setupSwipeListener(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false // We don't want move operations
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = (binding.rvRouteList.adapter as RouteAdapter).currentList[viewHolder.adapterPosition]
                viewModel.deleteRoute(item.id) // Assuming deleteRoute is implemented in ViewModel
            }
        }
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun setupClickListener(adapter: RouteAdapter) {
        adapter.onItemClick = { route ->
//            val action =
//                RoutelistFragmentDirections.actionRoutelistFragmentToEditRouteFragment(route.id)
//            findNavController().navigate(action)
        }
    }
}