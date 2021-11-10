package com.linhnvt.project_prm.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.linhnvt.project_prm.R
import com.linhnvt.project_prm.model.Song
import com.linhnvt.project_prm.repository.MusicRepository
import com.linhnvt.project_prm.ui.MainActivity
import com.linhnvt.project_prm.utils.Constant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MusicService : Service() {
    companion object {
        private const val MUSIC_SERVICE_TITLE = "music service"
        private const val SERVICE_NOTIFICATION_ID = 2907
    }

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
        isLooping = true
    }

    private val binder = LocalBinder()
    private var songList: ArrayList<Song> = ArrayList()
    private var currentSongIndex: Int = -1
    private var pendingForFetchingSong: Int = -1
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val op = intent?.getStringExtra(Constant.KEY_OPERATION)
        // check if this intent was sent from notification or not
        if (op != null) {
            when (op) {
                Constant.FAST_REWIND -> {
                    notifyChangeToUi(Constant.FAST_REWIND)
                    pickPrevSong(true)
                }
                Constant.ResumeOrPAUSE -> {
                    notifyChangeToUi(Constant.ResumeOrPAUSE)
                    resumeOrPauseMediaPlayer()
                }
                Constant.FAST_FORWARD -> {
                    notifyChangeToUi(Constant.FAST_FORWARD)
                    pickPriorSong(true)
                }
            }
        } else {
            createNotificationChannel()
            setUpMediaPlayer()
            startNewService()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            var name = MainActivity.MUSIC_PLAYER_CHANNEL_NAME
            var descriptionText = MainActivity.MUSIC_PLAYER_CHANNEL_DESCRIPTION_TEXT
            var importance = NotificationManager.IMPORTANCE_MIN
            var channel = NotificationChannel(MainActivity.MUSIC_PLAYER_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)

            name = MainActivity.NORMAL_CHANNEL_NAME
            descriptionText = MainActivity.NORMAL_CHANNEL_DESCRIPTION_TEXT
            importance = NotificationManager.IMPORTANCE_HIGH
            channel = NotificationChannel(MainActivity.NORMAL_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setUpMediaPlayer() {
        mediaPlayer.setOnPreparedListener {
            it.start()
            notifyChangeToUi(Constant.ResumeOrPAUSE)
        }

        mediaPlayer.setOnErrorListener { mp, i1, i2 ->
            Log.i(Constant.COMMON_TAG, "ERROR: ${mp.isPlaying} $i1 $i2")
            true
        }

        mediaPlayer.setOnCompletionListener {
            it.start()
        }
    }

    private fun startNewService() {
        val notificationLayout = setUpNotificationLayout()

        notificationBuilder = NotificationCompat.Builder(this, MainActivity.MUSIC_PLAYER_CHANNEL_ID)
            .setContentTitle(MUSIC_SERVICE_TITLE)
            .setSmallIcon(R.drawable.ic_music)
            .setCustomContentView(notificationLayout)
            .setNotificationSilent()
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true).setWhen(0)
            .setPriority(NotificationCompat.PRIORITY_MIN);


        startForeground(SERVICE_NOTIFICATION_ID, notificationBuilder?.build())
    }

    private fun notifyChangeToUi(op: String) {
        Intent(applicationContext, MainActivity::class.java).also {
            it.putExtra(Constant.KEY_OPERATION, op)
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            it.action = Intent.ACTION_MAIN
            it.addCategory(Intent.CATEGORY_LAUNCHER)
            startActivity(it)
        }
    }

    private fun setUpNotificationLayout(): RemoteViews {
        val notificationLayout = RemoteViews(packageName, R.layout.notification_layout)
        notificationLayout.setOnClickPendingIntent(
            R.id.btnFFLeftNoti,
            getPendingIntentForOnClick(R.id.btnFFLeftNoti, Constant.FAST_REWIND)
        )

        notificationLayout.setOnClickPendingIntent(
            R.id.btnResumeOrPauseNoti,
            getPendingIntentForOnClick(R.id.btnResumeOrPauseNoti, Constant.ResumeOrPAUSE)
        )

        notificationLayout.setOnClickPendingIntent(
            R.id.btnFFRightNoti,
            getPendingIntentForOnClick(R.id.btnFFRightNoti, Constant.FAST_FORWARD)
        )

        return notificationLayout
    }

    private fun getPendingIntentForOnClick(resID: Int, op: String): PendingIntent {
        val intent = Intent(applicationContext, MusicService::class.java)
        intent.putExtra(Constant.KEY_OPERATION, op)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getService(applicationContext, resID, intent, 0)
    }

    private fun changeNotificationViewItem(
        resIDs: ArrayList<Int>,
        text: ArrayList<String>,
        srcID: ArrayList<Int>
    ) {
        val notificationLayout = setUpNotificationLayout()
        resIDs.forEachIndexed { ind, item ->
            if (ind > text.size - 1)
                notificationLayout.setImageViewResource(item, srcID[ind - text.size])
            else if (text.isNotEmpty())
                notificationLayout.setTextViewText(item, text[ind])
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(
            SERVICE_NOTIFICATION_ID,
            notificationBuilder?.setCustomContentView(notificationLayout)?.build()
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    fun stopMusicService() {
        mediaPlayer.stop()
        mediaPlayer.release()
        stopSelf()
    }

    fun getMediaPlayerDuration() = mediaPlayer.duration

    fun getMediaPlayerCurrentPosition() = mediaPlayer.currentPosition

    fun isMediaPlayerPlaying() = mediaPlayer.isPlaying

    fun mediaPlayerSeekTo(msec: Int) = mediaPlayer.seekTo(msec)

    private fun mediaPlayerPlaySong(song: Song) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }

        mediaPlayer.reset()
        mediaPlayer.setDataSource(applicationContext, song.songUri)
        mediaPlayer.prepareAsync()

        changeNotificationViewItem(
            arrayListOf(
                R.id.tvSongNameInNoti,
                R.id.tvSongInfoInNoti,
                R.id.btnResumeOrPauseNoti
            ),
            arrayListOf(song.name, "${song.artis} - ${song.description}"),
            arrayListOf(R.drawable.ic_pause)
        )
    }

    fun pickPrevSong(isFromNotification: Boolean = false): Song? {
        if (this.currentSongIndex != -1) {
            val index =
                if (this.currentSongIndex == 0)
                    songList.size - 1
                else
                    this.currentSongIndex - 1

            songList[index].also {
                if (it.songUri != Uri.EMPTY) {
                    currentSongIndex = index
                    mediaPlayerPlaySong(it)
                }else if(isFromNotification){
                    coroutineScope.launch {
                        MusicRepository().fetchFileFromFirebase(it.fileName).also { songUriChannel ->
                            it.songUri = songUriChannel.receive() ?: Uri.EMPTY
                            mediaPlayerPlaySong(it)
                            setPendingSongToPlay(it.id)
                        }
                    }
                }
                return@pickPrevSong songList[index]
            }
        }
        return null
    }

    fun resumeOrPauseMediaPlayer() {
        if (isMediaPlayerPlaying()) {
            mediaPlayer.pause()
            changeNotificationViewItem(
                arrayListOf(R.id.btnResumeOrPauseNoti),
                arrayListOf(),
                arrayListOf(R.drawable.ic_play),
            )
        } else {
            mediaPlayer.start()
            changeNotificationViewItem(
                arrayListOf(R.id.btnResumeOrPauseNoti),
                arrayListOf(),
                arrayListOf(R.drawable.ic_pause),
            )
        }
    }

    fun pickPriorSong(isFromNotification: Boolean = false): Song? {
        if (this.currentSongIndex != -1) {
            val index =
                if (this.currentSongIndex == songList.size - 1)
                    0
                else
                    this.currentSongIndex + 1

            songList[index].also {
                currentSongIndex = index
                if (it.songUri != Uri.EMPTY) {
                    mediaPlayerPlaySong(it)
                }else if(isFromNotification){
                    coroutineScope.launch {
                        MusicRepository().fetchFileFromFirebase(it.fileName).also { songUriChannel ->
                            it.songUri = songUriChannel.receive() ?: Uri.EMPTY
                            mediaPlayerPlaySong(it)
                        }
                    }
                }
                return@pickPriorSong songList[index]
            }
        }
        return null
    }

    fun setCurrentSong(songId: String) {
        val song = songList.firstOrNull { it.id == songId }
        currentSongIndex = if (song != null) songList.indexOf(song) else -1
        mediaPlayerPlaySong(songList[currentSongIndex])

        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.putExtra(Constant.KEY_OPERATION, Constant.UPDATE_CURRENT_SONG)
        intent.putExtra(Constant.KEY_INDEX, currentSongIndex)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)
    }

    fun setPendingSongToPlay(songId: String) {
        val song = songList.firstOrNull { it.id == songId }
        pendingForFetchingSong = if (song != null) songList.indexOf(song) else -1
    }

    private fun isPendingSong(songId: String): Boolean {
        val song = songList.firstOrNull { it.id == songId }
        return if (pendingForFetchingSong == -1)
            false
        else
            pendingForFetchingSong == if (song != null) songList.indexOf(song) else -1
    }

    fun getPendingSong(): Song? {
        return if( pendingForFetchingSong == -1)
            null
        else
            songList[pendingForFetchingSong]
    }

    fun getCurrentSong(): Song? {
        return if (currentSongIndex != -1)
            songList[currentSongIndex]
        else
            null
    }

    fun getSong(index: Int): Song? {
        return if (index >= 0 && index < songList.size)
            songList[index]
        else
            null
    }

    fun addSong(songs: ArrayList<Song>) {
        songList.addAll(songs)
    }

    fun addSongUri(fetchedSong: Song?) {
        val song = songList.firstOrNull { it.id == fetchedSong?.id }
        if (song != null) {
            song.songUri = fetchedSong!!.songUri
            song.isUriLoaded = true
            if (isPendingSong(song.id)) {
                pendingForFetchingSong = -1
                setCurrentSong(song.id)
            }
        }
    }
}