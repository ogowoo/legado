package io.dushu.app.ui.book.changecover

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.dushu.app.base.adapter.DiffRecyclerAdapter
import io.dushu.app.base.adapter.ItemViewHolder
import io.dushu.app.data.entities.SearchBook
import io.dushu.app.databinding.ItemCoverBinding


class CoverAdapter(context: Context, val callBack: CallBack) :
    DiffRecyclerAdapter<SearchBook, ItemCoverBinding>(context) {

    override val diffItemCallback: DiffUtil.ItemCallback<SearchBook>
        get() = object : DiffUtil.ItemCallback<SearchBook>() {
            override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
            }

            override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return oldItem.originName == newItem.originName
                        && oldItem.coverUrl == newItem.coverUrl
            }

        }

    override fun getViewBinding(parent: ViewGroup): ItemCoverBinding {
        return ItemCoverBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemCoverBinding,
        item: SearchBook,
        payloads: MutableList<Any>
    ) = binding.run {
        ivCover.load(item, false)
        tvSource.text = item.originName
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemCoverBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.changeTo(it.coverUrl ?: "")
                }
            }
        }
    }

    interface CallBack {
        fun changeTo(coverUrl: String)
    }
}