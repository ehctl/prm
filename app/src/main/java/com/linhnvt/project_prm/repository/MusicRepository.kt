package com.linhnvt.project_prm.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.linhnvt.project_prm.model.Song
import com.linhnvt.project_prm.utils.Constant
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*

class MusicRepository {
    companion object {
        private const val KEY_SONG_ID = "_id"
        private const val KEY_SONG_NAME = "name"
        private const val KEY_SONG_ARTIS = "artis"
        private const val KEY_SONG_DESCRIPTION = "description"
        private const val KEY_SONG_FILE_NAME = "file_name"
        private const val KEY_SONG_DURATION = "duration"
        private const val FIREBASE_DB_SONG_COLLECTION_PATH = "song"
    }

    private var firebaseDb = Firebase.firestore
    private var coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun fetchSongListFromFirebase(dispatcher: CoroutineDispatcher = Dispatchers.IO): Channel<ArrayList<Song>> {
        val songChannel = Channel<ArrayList<Song>>()
        coroutineScope.launch(dispatcher) {
            firebaseDb.collection(FIREBASE_DB_SONG_COLLECTION_PATH)
                .get()
                .addOnCompleteListener {
                    generateSong(songChannel, it.result?.documents, dispatcher)
                }
                .addOnFailureListener {
                    Log.i(Constant.COMMON_TAG, "${it.message}")
                }
        }

        return songChannel
    }

    private fun generateSong(
        songChannel: Channel<ArrayList<Song>>,
        documents: List<DocumentSnapshot>?,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        coroutineScope.launch(dispatcher) {
            val songList = arrayListOf<Song>()
            documents?.forEach { document ->
                songList.add(
                    Song(
                        id = document.data?.get(KEY_SONG_ID).toString(),
                        name = document.data?.get(KEY_SONG_NAME).toString(),
                        artis = document.data?.get(KEY_SONG_ARTIS).toString(),
                        description = document.data?.get(KEY_SONG_DESCRIPTION).toString(),
                        fileName = document.data?.get(KEY_SONG_FILE_NAME).toString(),
                        duration = document.data?.get(KEY_SONG_DURATION).toString()
                            .toIntOrNull() ?: 0,
                        isLoaded = true
                    )
                )
            }

            fetchSongImage(songChannel, songList)
        }
    }

    private suspend fun fetchSongImage(
        songChannel: Channel<ArrayList<Song>>,
        songList: ArrayList<Song>
    ) {
        val imageUriChannel = Channel<Uri?>()
        coroutineScope.launch {
            songList.forEach {
                fetchFileFromFirebase(it.id.plus(Constant.JPG_EXTENSION), imageUriChannel)
            }
        }

        for(i in 0 until songList.size) {
            val imageUri = imageUriChannel.receive()
            if (imageUri != null)
                songList.firstOrNull { imageUri.toString().contains(it.id) }?.imageUri = imageUri
        }

        songChannel.send(songList)
    }

    suspend fun fetchFileFromFirebase(
        fileName: String,
        givenUriChannel: Channel<Uri?>? = null,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Channel<Uri?> {
        val storage = FirebaseStorage.getInstance()
        val fileChannel = givenUriChannel ?: Channel()
        val storageRef =
            storage.reference.child(Constant.STRING_SLASH + fileName)
        storageRef.downloadUrl
            .addOnFailureListener { e ->
                Log.i(Constant.COMMON_TAG, "${e.message}")
                coroutineScope.launch(dispatcher){
                    fileChannel.send(null)
                }
            }
            .addOnCompleteListener {
                coroutineScope.launch(dispatcher){
                    fileChannel.send(it.result)
                }
            }

        return fileChannel
    }

}