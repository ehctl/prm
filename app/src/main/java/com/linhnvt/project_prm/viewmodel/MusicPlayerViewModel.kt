package com.linhnvt.project_prm.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linhnvt.project_prm.model.Song
import com.linhnvt.project_prm.repository.MusicRepository
import com.linhnvt.project_prm.ui.fragment.MusicListFragment
import com.linhnvt.project_prm.ui.fragment.MusicPlayerFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.Normalizer

class MusicPlayerViewModel : ViewModel() {
    private val musicRepository = MusicRepository()

    private var _newComingIntent = MutableLiveData<Intent?>()
    val newComingIntent: LiveData<Intent?>
        get() = _newComingIntent

    fun setNewComingIntent(intent: Intent?) {
        _newComingIntent.postValue(intent)
    }

    private var songListMask = ArrayList<Song>()
    private var _songList = MutableLiveData<ArrayList<Song>>()
    val songList: LiveData<ArrayList<Song>>
        get() = _songList

    private var _fetchedSongUri = MutableLiveData<Song>()
    val fetchedSongUri: LiveData<Song>
        get() = _fetchedSongUri

    init {
        Song().let {
            for (i in 0..MusicListFragment.PRELOAD_FAKE_SONG)
                songListMask.add(it)
        }
        _songList.postValue(songListMask)
    }

    fun loadingSongList() {
        viewModelScope.launch(Dispatchers.Main) {
            if( songListMask.size > 0) {
                songListMask.forEach { it.isLoaded = false }
                _songList.postValue(songListMask)
            }

            musicRepository.fetchSongListFromFirebase().also {
                val tempSongList = it.receive()
                songListMask.clear()
                songListMask.addAll(tempSongList)
                _songList.postValue(songListMask)
            }
        }
    }

    fun fetchSongUri(song: Song) {
        if (song.id != MusicPlayerFragment.STRING_MINUS_ONE)
            viewModelScope.launch(Dispatchers.Main) {
                musicRepository.fetchFileFromFirebase(song.fileName).also {
                    val songUri = it.receive() ?: Uri.EMPTY
                    _fetchedSongUri.postValue(
                        Song(
                            song.id,
                            fileName = song.fileName,
                            songUri = songUri
                        )
                    )
                    song.isUriLoaded = true
                }
            }
    }

    fun searchSongList(text: String?) {
        if (!text.isNullOrBlank()) {
            _songList.postValue(
                ArrayList(songListMask.filter { song ->
                    song.artis.contains(text, true) ||
                            song.description.contains(text, true) ||
                            song.name.contains(text, true)
                })
            )
        } else {
            _songList.postValue(songListMask)
        }
    }
}