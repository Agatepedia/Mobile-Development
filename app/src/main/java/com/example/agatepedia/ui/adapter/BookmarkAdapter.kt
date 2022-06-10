package com.example.agatepedia.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agatepedia.data.local.entity.AgateEntity
import com.example.agatepedia.data.remote.response.AgateResponseItem
import com.example.agatepedia.databinding.ItemListAgeteBinding
import com.example.agatepedia.databinding.ItemListBookmarkBinding

class BookmarkAdapter: ListAdapter< AgateEntity, BookmarkAdapter.ViewHolder>(DiffCallBack) {

    var onItemClick: ((AgateEntity, View) -> Unit)? = null

    class ViewHolder(val binding: ItemListBookmarkBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): BookmarkAdapter.ViewHolder {
        val binding =
            ItemListBookmarkBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkAdapter.ViewHolder, position: Int) {
        val agate = getItem(position)
        val binding = ItemListAgeteBinding.bind(holder.itemView)

        Glide.with(holder.itemView.context)
            .load(agate.image)
            .circleCrop()
            .into(binding.agetePhoto)

        with(binding){
            agateName.text = agate.type
            agatePrice.text = "${agate.price}"
            root.setOnClickListener{
                onItemClick?.invoke(agate, holder.itemView)
            }
        }
    }

    object DiffCallBack : DiffUtil.ItemCallback<AgateEntity>() {
        override fun areItemsTheSame(oldItem: AgateEntity, newItem: AgateEntity): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: AgateEntity, newItem: AgateEntity): Boolean {
            return oldItem.price == newItem.price
        }


    }
}