package com.cleaningos.domain.repository

import com.cleaningos.domain.model.*
import kotlinx.coroutines.flow.Flow

/** Knowledge base and search */
interface SearchRepository {
    suspend fun query(text: String): SearchResult
    suspend fun searchKnowledgeBases(query: String): List<KBResult>
    fun getKnowledgeBases(): Flow<List<KBResult>>
}

/** Cleaning objects / locations */
interface ObjectRepository {
    fun getAllObjects(): Flow<List<CleaningObject>>
    suspend fun getObjectById(id: Long): CleaningObject?
    suspend fun addObject(obj: CleaningObject): Long
    suspend fun updateCoords(id: Long, lat: Double, lon: Double)
    suspend fun deleteObject(id: Long)
}

/**
 * MediaRepository — commonMain interface.
 * Implemented via Expect/Actual in androidMain (MediaStore) and iosMain (MPMediaLibrary).
 */
interface MediaRepository {
    /** Scan device storage and return all audio tracks */
    suspend fun scanAudioFiles(): List<AudioTrack>
    /** Observe changes to the library */
    fun observeLibrary(): Flow<List<AudioTrack>>
}

/**
 * VoiceRepository — commonMain interface.
 * Android: SpeechRecognizer. iOS: SFSpeechRecognizer.
 */
interface VoiceRepository {
    fun startListening(): Flow<VoiceState>
    fun stopListening()
    val isAvailable: Boolean
}

/** Audio player control — expect/actual bridged */
interface AudioPlayerController {
    fun play(track: AudioTrack)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun next()
    fun previous()
    fun setVolume(volume: Float)
    fun observeState(): Flow<MusicState>
}
