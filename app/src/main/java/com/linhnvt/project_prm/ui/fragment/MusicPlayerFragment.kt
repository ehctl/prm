package com.linhnvt.project_prm.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import com.linhnvt.project_prm.R
import com.linhnvt.project_prm.base.BaseFragment
import com.linhnvt.project_prm.databinding.FragmentVideoPlayerBinding
import com.linhnvt.project_prm.model.Song
import com.linhnvt.project_prm.service.MusicService
import com.linhnvt.project_prm.utils.Constant
import com.linhnvt.project_prm.utils.Helper
import com.linhnvt.project_prm.viewmodel.AppViewModel
import com.linhnvt.project_prm.viewmodel.MusicPlayerViewModel
import kotlinx.coroutines.*

@SuppressLint("SetTextI18n")
class MusicPlayerFragment : BaseFragment<FragmentVideoPlayerBinding>() {
    companion object {
        private const val SEEKBAR_MAX_PROGRESS = 100
        private const val PROGRESS_TIMER_DELAY = 500L
        private const val TIME_COUNTER_DELAY = 1000L
        const val STRING_MINUS_ONE = "-1"

        @JvmStatic
        fun newInstance() = MusicPlayerFragment()
    }

    private val appViewModel by activityViewModels<AppViewModel>()
    private val musicPlayerViewModel by activityViewModels<MusicPlayerViewModel>()

    private var prevTimeCounter: Job? = null
    private var prevProgressCounter: Job? = null
    private val coroutineScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var mService: MusicService? = null

    private var mBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            mService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }
    }

    override fun onDestroy() {
        mService?.stopMusicService()
        coroutineScope.cancel()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun initData() {
        Intent(context, MusicService::class.java).also { intent ->
            activity?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        updateCurrentSong(null)
    }

    override fun initAction() {
        binding.musicPlayerContainer.setOnClickListener {
            View.OnTouchListener { v, event ->
                v?.onTouchEvent(event)
                v.performClick()
                true
            }
        }
        binding.btnFFLeft.setOnClickListener(btnFFLeftOnClickListener)
        binding.btnFFRight.setOnClickListener(btnFFRightOnClickListener)
        binding.btnResumeOrPause.setOnClickListener(btnResumeOrPauseClickListener)
        binding.sbTime.setOnSeekBarChangeListener(seekbarOnChangeListener)

        musicPlayerViewModel.newComingIntent.observe(viewLifecycleOwner) {
            handleNewIntentFromService(it)
        }
    }

    private val seekbarOnChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                mService?.mediaPlayerSeekTo(
                    (progress.toFloat() / SEEKBAR_MAX_PROGRESS * mService?.getMediaPlayerDuration()!!).toInt()
                )

            }
            seekBar?.progress = progress
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    }

    private val btnResumeOrPauseClickListener = View.OnClickListener {
        btnFFResumeOrPauseOnClick()
    }

    private val btnFFLeftOnClickListener = View.OnClickListener {
        btnFFLeftOnClick()
    }

    private val btnFFRightOnClickListener = View.OnClickListener {
        btnFFRightOnClick()
    }

    private fun handleNewIntentFromService(intent: Intent?) {
        val op = intent?.getStringExtra(Constant.KEY_OPERATION)
        // check if this intent was sent from notification or not
        if (op != null) {
            when (op) {
                Constant.FAST_REWIND, Constant.FAST_FORWARD -> {
                    updateCurrentSongUI()
                }
                Constant.ResumeOrPAUSE -> {
                    updateBtnResumeOrPauseUI()
                }
                Constant.UPDATE_CURRENT_SONG -> {
                    val index = intent.getIntExtra(Constant.KEY_INDEX, -1)
                    updateCurrentSong(index)
                }
            }
        }
    }

    private fun btnFFResumeOrPauseOnClick() {
        mService?.resumeOrPauseMediaPlayer()
        updateBtnResumeOrPauseUI()
    }

    private fun btnFFLeftOnClick() {
        val song = mService?.pickPrevSong()
        updateCurrentSongUI()
        if (song?.songUri == Uri.EMPTY) {
            appViewModel.isLoading(true)
            mService?.setPendingSongToPlay(song?.id ?: STRING_MINUS_ONE)
            musicPlayerViewModel.fetchSongUri(song ?: Song())
        }
    }

    private fun btnFFRightOnClick() {
        val song = mService?.pickPriorSong()
        updateCurrentSongUI()
        if (song?.songUri == Uri.EMPTY) {
            appViewModel.isLoading(true)
            mService?.setPendingSongToPlay(song?.id ?: STRING_MINUS_ONE)
            musicPlayerViewModel.fetchSongUri(song ?: Song())
        }
    }

    private fun updateBtnResumeOrPauseUI() {
        if (mService?.isMediaPlayerPlaying() == true) {
            binding.btnResumeOrPause.setImageResource(R.drawable.ic_pause)
        } else {
            binding.btnResumeOrPause.setImageResource(R.drawable.ic_play)
        }
    }

    private fun updateCurrentSongUI() {
        mService?.getPendingSong().also {
            if (it == null)
                mService?.getCurrentSong()?.let { song -> updateCurrentSong(song) }
            else {
                mService?.setPendingSongToPlay(STRING_MINUS_ONE)
                updateCurrentSong(it)
            }
        }
    }

    private fun updateCurrentSong(id: Int) {
        val song = mService?.getSong(id)
        updateCurrentSong(song)
    }

    override fun onDetach() {
        if (prevProgressCounter?.isActive == true) prevProgressCounter?.cancel()
        if (prevTimeCounter?.isActive == true) prevTimeCounter?.cancel()
        super.onDetach()
    }

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentVideoPlayerBinding {
        return FragmentVideoPlayerBinding.inflate(inflater, container, false)
    }

    override fun releaseData() {
        activity?.unbindService(connection)
        mBound = false
    }

    private fun updateCurrentSong(song: Song?) {
        binding.tvCurrentTime.text = getString(R.string.start_timer)
        binding.sbTime.max = SEEKBAR_MAX_PROGRESS
        binding.sbTime.progress = 0
        if (song == null) {
            binding.tvSongName.text = getString(R.string.no_song_selected)
            binding.tvDuration.text = getString(R.string.start_timer)
        } else {
//            if( lifecycle.currentState.isAtLeast(Lifecycle.State.))
            binding.tvSongName.text = "${song.name} - ${song.artis}"
            binding.tvDuration.text = Helper.convertTime(song.duration)
            startTimeCounter()
            Log.i(Constant.COMMON_TAG, "${binding.tvSongName.text} ${song.name} - ${song.artis}")

        }
    }

    private fun startTimeCounter() {
        if (prevProgressCounter?.isActive == true) prevProgressCounter?.cancel()
        prevProgressCounter = coroutineScope.launch(Dispatchers.Default) {
            while (true) {
                if (mService?.isMediaPlayerPlaying() == true) {
                    val progress =
                        (mService?.getMediaPlayerCurrentPosition()?.toFloat() ?: 0F) /
                                (mService?.getMediaPlayerDuration() ?: 0) * SEEKBAR_MAX_PROGRESS

                    withContext(Dispatchers.Main) {
                        binding.sbTime.progress = progress.toInt()
                    }
                }
                delay(PROGRESS_TIMER_DELAY)
            }
        }

        if (prevTimeCounter?.isActive == true) prevTimeCounter?.cancel()
        prevTimeCounter = coroutineScope.launch(Dispatchers.Default) {
            while (true) {
                if (mService?.isMediaPlayerPlaying() == true) {
                    val currentTime = mService?.getMediaPlayerCurrentPosition()?.div(
                        TIME_COUNTER_DELAY.toInt()
                    )?.let {
                        Helper.convertTime(it)
                    }

                    withContext(Dispatchers.Main) {
                        binding.tvCurrentTime.text = currentTime
                    }
                }
                delay(TIME_COUNTER_DELAY)
            }
        }
    }
}
