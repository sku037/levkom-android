package com.example.levkomandroid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.levkomandroid.R
import com.example.levkomandroid.network.RouteDto

class RouteAdapter : ListAdapter<RouteDto, RouteAdapter.RouteViewHolder>(DiffCallback) {
    var onItemClick: ((RouteDto) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = getItem(position)
        holder.bind(route)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(route)  // Pass route directly from here
        }
    }

    class RouteViewHolder(itemView: View, private val onItemClick: ((RouteDto) -> Unit)?) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvName)
        private val startAddressTextView: TextView = itemView.findViewById(R.id.tvStartAddress)
        private val endAddressTextView: TextView = itemView.findViewById(R.id.tvEndAddress)

        fun bind(route: RouteDto) {
            nameTextView.text = route.name
            startAddressTextView.text = route.startAddress
            endAddressTextView.text = route.endAddress
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RouteDto>() {
            override fun areItemsTheSame(oldItem: RouteDto, newItem: RouteDto): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: RouteDto, newItem: RouteDto): Boolean {
                return oldItem == newItem
            }
        }
    }
}