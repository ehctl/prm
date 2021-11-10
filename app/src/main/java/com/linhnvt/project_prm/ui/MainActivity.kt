package com.linhnvt.project_prm.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import com.linhnvt.project_prm.R
import com.linhnvt.project_prm.base.BaseActivity
import com.linhnvt.project_prm.databinding.ActivityMainBinding
import com.linhnvt.project_prm.service.MusicService
import com.linhnvt.project_prm.viewmodel.AppViewModel
import com.linhnvt.project_prm.viewmodel.MusicPlayerViewModel


class MainActivity : BaseActivity<ActivityMainBinding>() {
    companion object {
        const val MUSIC_PLAYER_CHANNEL_ID = "music channel"
        const val MUSIC_PLAYER_CHANNEL_NAME = "music channel"
        const val MUSIC_PLAYER_CHANNEL_DESCRIPTION_TEXT = "notification channel "
        const val NORMAL_CHANNEL_ID = "normal channel"
        const val NORMAL_CHANNEL_NAME = "normal channel"
        const val NORMAL_CHANNEL_DESCRIPTION_TEXT = "notification channel "
    }

    private val appViewModel by viewModels<AppViewModel>()
    private val musicPlayerViewModel by viewModels<MusicPlayerViewModel>()

    override fun setUpActivity(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        startMusicService()
    }

    override fun createBinding(
        inflater: LayoutInflater,
    ): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun initAction() {
        appViewModel.isLoading.observe(this) {
            if (it) {
                binding.llLoading.visibility = View.VISIBLE
                binding.container.isClickable = false
            } else {
                binding.llLoading.visibility = View.GONE
                binding.container.isClickable = true
            }
        }
    }

    private fun startMusicService() {
        val intent = Intent(this, MusicService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent)
        else
            startService(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        musicPlayerViewModel.setNewComingIntent(intent)
    }
}
