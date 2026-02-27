package com.cleaningos.media.repository

import android.content.Context
import android.provider.MediaStore
import com.cleaningos.domain.model.AudioTrack
import com.cleaningos.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * MediaRepositoryImpl — Android MediaStore implementation.
 * Scans device storage for audio files and returns them as AudioTrack domain objects.
 */
class MediaRepositoryImpl(private val context: Context) : MediaRepository {

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.ALBUM_ID
    )

    override suspend fun scanAudioFiles(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",  // music files only
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        ) ?: return@withContext emptyList()

        cursor.use {
            val idCol       = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol     = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdCol  = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val id      = it.getLong(idCol)
                val albumId = it.getLong(albumIdCol)
                tracks += AudioTrack(
                    id           = id,
                    title        = it.getString(titleCol)  ?: "Unknown",
                    artist       = it.getString(artistCol) ?: "Unknown Artist",
                    album        = it.getString(albumCol)  ?: "Unknown Album",
                    durationMs   = it.getLong(durationCol),
                    uri          = "content://media/external/audio/media/$id",
                    albumArtUri  = "content://media/external/audio/albumart/$albumId"
                )
            }
        }
        tracks
    }

    override fun observeLibrary(): Flow<List<AudioTrack>> = callbackFlow {
        // Initial load
        trySend(scanAudioFiles())

        // ContentObserver for live updates
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    trySend(scanAudioFiles())
                }
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
}
