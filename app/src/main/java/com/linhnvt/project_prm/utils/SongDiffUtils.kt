package com.linhnvt.project_prm.utils

import androidx.recyclerview.widget.DiffUtil
import com.linhnvt.project_prm.model.Song

class SongDiffUtils (
    private val oldData: List<Song>,
    private val newData: List<Song>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldData.size

    override fun getNewListSize() = newData.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldData[oldItemPosition]
        val newItem = newData[newItemPosition]

        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldData[oldItemPosition]
        val newItem = newData[newItemPosition]

        return oldItem.id == newItem.id
    }
}