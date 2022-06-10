package com.example.agatepedia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agatepedia.data.remote.response.AgateResponseItem
import com.example.agatepedia.databinding.ItemListAgeteBinding

class AgateAdapter : ListAdapter<AgateResponseItem, AgateAdapter.ViewHolder>(DiffCallBack) {

    var onItemClick: ((AgateResponseItem, View) -> Unit)? = null

    class ViewHolder(val binding: ItemListAgeteBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): AgateAdapter.ViewHolder {
        val binding =
            ItemListAgeteBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AgateAdapter.ViewHolder, position: Int) {
        val agate = getItem(position)
        val binding = ItemListAgeteBinding.bind(holder.itemView)

        Glide.with(holder.itemView.context)
            .load(agate.gambar)
            .circleCrop()
            .into(binding.agetePhoto)

        with(binding){
            agateName.text = agate.jenis
            agatePrice.text = "${agate.harga}"
            root.setOnClickListener{
                onItemClick?.invoke(agate, holder.itemView)
            }
        }
    }

    object DiffCallBack : DiffUtil.ItemCallback<AgateResponseItem>() {
        override fun areItemsTheSame(
            oldItem: AgateResponseItem,
            newItem: AgateResponseItem
        ): Boolean {
            return oldItem.jenis == newItem.jenis
        }

        override fun areContentsTheSame(
            oldItem: AgateResponseItem,
            newItem: AgateResponseItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

    }

}