package com.linhnvt.project_prm.ui.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.linhnvt.project_prm.base.BaseFragment
import com.linhnvt.project_prm.databinding.FragmentMusicListBinding
import com.linhnvt.project_prm.model.Song
import com.linhnvt.project_prm.service.MusicService
import com.linhnvt.project_prm.ui.adapter.MusicListAdapter
import com.linhnvt.project_prm.utils.Constant
import com.linhnvt.project_prm.viewmodel.AppViewModel
import com.linhnvt.project_prm.viewmodel.MusicPlayerViewModel

class MusicListFragment : BaseFragment<FragmentMusicListBinding>() {
    companion object {
        @JvmStatic
        fun newInstance() = MusicListFragment()

        const val PRELOAD_FAKE_SONG = 30
    }

    private val appViewModel by activityViewModels<AppViewModel>()
    private val musicPlayerViewModel by activityViewModels<MusicPlayerViewModel>()
    private var mService: MusicService? = null
    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            mService = binder.getService()
            fetchSongList()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }
    }

    override fun onDestroy() {
        mService?.stopMusicService()
        super.onDestroy()
    }

    override fun releaseData() {
        activity?.unbindService(connection)
        mBound = false
    }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentMusicListBinding {
        return FragmentMusicListBinding.inflate(inflater, container, false)
    }

    override fun initAction() {
        binding.tvSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                musicPlayerViewModel.searchSongList(s?.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        musicPlayerViewModel.songList.observe(viewLifecycleOwner) { songList ->
            mService?.addSong(songList)
            (binding.rvMusicList.adapter as MusicListAdapter).setData(songList)
            binding.ptrMain.isRefreshing = false
        }

        musicPlayerViewModel.fetchedSongUri.observe(viewLifecycleOwner){ song ->
            mService?.addSongUri(song)
            appViewModel.isLoading(false)
        }

        binding.ptrMain.setOnRefreshListener {
            fetchSongList()
        }
    }

    override fun initData() {
        MusicListAdapter().let {
            it.songList = arrayListOf()
            it.itemOnClickCallback = { selectedSong ->
                if(selectedSong.isUriLoaded)
                    mService?.setCurrentSong(selectedSong.id)
                else {
                    mService?.setPendingSongToPlay(selectedSong.id)
                    fetchSongUri(selectedSong)
                }
            }
            binding.rvMusicList.adapter = it
        }
        binding.rvMusicList.setHasFixedSize(true)
        binding.rvMusicList.layoutManager = LinearLayoutManager(context)
        Intent(context, MusicService::class.java).also { intent ->
            activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun fetchSongUri(song: Song) {
        appViewModel.isLoading(true)
        musicPlayerViewModel.fetchSongUri(song)
    }

    private fun fetchSongList() {
        musicPlayerViewModel.loadingSongList()
    }
}
