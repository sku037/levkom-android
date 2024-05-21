package com.example.levkomandroid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.levkomandroid.R
import com.example.levkomandroid.network.DeliveryAddr

class AddressAdapter : ListAdapter<DeliveryAddr, AddressAdapter.AddressViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = getItem(position)
        holder.bind(address, position)
    }

    class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewAddress: TextView = itemView.findViewById(R.id.textViewAddress)

        fun bind(address: DeliveryAddr, position: Int) {
            textViewAddress.text = "${position + 1}. ${address.street}, ${address.hnr}, ${address.post}, ${address.city}"
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<DeliveryAddr>() {
            override fun areItemsTheSame(oldItem: DeliveryAddr, newItem: DeliveryAddr): Boolean {
                // Assuming there is a unique identifier in Address model to compare
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: DeliveryAddr, newItem: DeliveryAddr): Boolean {
                return oldItem == newItem
            }
        }
    }
}