package com.cleaningos.core.di

import com.cleaningos.domain.repository.AudioPlayerController
import com.cleaningos.domain.repository.MediaRepository
import com.cleaningos.domain.repository.VoiceRepository
import com.cleaningos.media.player.AndroidAudioPlayerController
import com.cleaningos.media.repository.MediaRepositoryImpl
import com.cleaningos.voice.repository.VoiceRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<MediaRepository>         { MediaRepositoryImpl(androidContext()) }
    single<AudioPlayerController>   { AndroidAudioPlayerController(androidContext()) }
    single<VoiceRepository>         { VoiceRepositoryImpl(androidContext()) }
}
