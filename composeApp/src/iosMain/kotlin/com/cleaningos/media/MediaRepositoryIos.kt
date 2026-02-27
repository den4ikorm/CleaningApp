package com.cleaningos.media

import com.cleaningos.domain.model.AudioTrack
import com.cleaningos.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** iOS stub — implement with MPMediaLibrary in production */
class MediaRepositoryIos : MediaRepository {
    override suspend fun scanAudioFiles(): List<AudioTrack> = emptyList()
    override fun observeLibrary(): Flow<List<AudioTrack>> = flowOf(emptyList())
}
