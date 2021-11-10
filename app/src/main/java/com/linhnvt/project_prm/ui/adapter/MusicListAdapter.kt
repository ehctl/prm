package com.linhnvt.project_prm.ui.adapter

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.linhnvt.project_prm.R
import com.linhnvt.project_prm.databinding.MusicItemBinding
import com.linhnvt.project_prm.model.Song
import com.linhnvt.project_prm.utils.Helper
import com.linhnvt.project_prm.utils.MyApp
import com.linhnvt.project_prm.utils.SongDiffUtils

@SuppressLint("SetTextI18n")
class MusicListAdapter() :
    RecyclerView.Adapter<MusicListAdapter.ViewHolder>() {

    var songList: ArrayList<Song> = arrayListOf()
    var itemOnClickCallback: ((Song) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicListAdapter.ViewHolder {
        val binding = MusicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(songList[position])
    }

    fun setData(newData: List<Song>) {
        try {
            val diffResult = DiffUtil.calculateDiff(
                SongDiffUtils(
                    songList.toList(),
                    newData
                )
            )

            diffResult.dispatchUpdatesTo(this)
            songList.clear()
            songList.addAll(newData)
        } catch (e: Exception) {
            songList.clear()
            songList.addAll(newData)
        }
    }

    override fun getItemCount(): Int = songList.size

    inner class ViewHolder(private val binding: MusicItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.tvSongInfo.text = "${song.name} - ${song.artis} - ${song.description}"
            itemView.setOnClickListener {
                itemOnClickCallback?.invoke(song)
            }

            Glide.with(itemView)
                .load(song.imageUri)
                .override(
                    MyApp.context().resources.getDimension(R.dimen.m84dp).toInt(),
                    MyApp.context().resources.getDimension(R.dimen.m84dp).toInt()
                )
                .fitCenter()
                .into(binding.ivSongImage)

            binding.shimmerAnim.also {
                if (!song.isLoaded) {
                    it.startLoadingAnimation()
                    it.visibility = View.VISIBLE
                } else {
                    it.stopLoadingAnimation()
                    it.visibility = View.GONE
                    binding.llItemContainer.background.setColorFilter(
                        MyApp.context().resources.getColor(
                            Helper.randomColor()
                        ), PorterDuff.Mode.MULTIPLY
                    )
                }
            }
        }
    }
}
