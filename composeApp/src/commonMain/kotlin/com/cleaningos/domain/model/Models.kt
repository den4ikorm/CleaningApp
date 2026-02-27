package com.cleaningos.domain.model

import kotlinx.serialization.Serializable

// ── Cleaning Object (Location) ────────────────────────────────────────────────
@Serializable
data class CleaningObject(
    val id: Long = 0,
    val name: String,
    val address: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val clientType: ClientType = ClientType.Standard,
    val notes: String = "",
    val visitCount: Int = 0,
    val lastVisit: String? = null
)

enum class ClientType { Standard, Demanding, Corporate, NewTenant }

// ── Checklist / Steps ─────────────────────────────────────────────────────────
@Serializable
data class Checklist(
    val id: String,
    val name: String,
    val estimatedMinutes: Int,
    val steps: List<ChecklistStep>,
    val warnings: List<String> = emptyList()
)

@Serializable
data class ChecklistStep(
    val text: String,
    val critical: Boolean = false,
    val minutes: Int = 0,
    var isDone: Boolean = false
)

// ── EDI State ─────────────────────────────────────────────────────────────────
@Serializable
data class EdiScore(
    val E: Int = 0,  // Energy 0-5
    val D: Int = 0,  // Demand 0-5
    val I: Int = 0,  // Intensity 0-5
    val flags: List<String> = emptyList(),
    val matched: String = ""
)

// ── Search Result ─────────────────────────────────────────────────────────────
data class SearchResult(
    val input: String,
    val edi: EdiScore,
    val checklist: Checklist?,
    val supportPhrase: String?,
    val chemicalWarning: ChemicalWarning?,
    val stainInfo: StainInfo?,
    val safetyAlert: Boolean = false,
    val alertText: String = "",
    val kbResults: List<KBResult> = emptyList()
)

data class ChemicalWarning(
    val reaction: String,
    val action: String,
    val isDeadly: Boolean
)

data class StainInfo(
    val name: String,
    val steps: List<String>
)

data class KBResult(
    val name: String,
    val title: String,
    val snippet: String,
    val score: Int
)

// ── Music / Media ─────────────────────────────────────────────────────────────
@Serializable
data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String? = null
)

data class MusicState(
    val playlist: List<AudioTrack> = emptyList(),
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ── Voice / STT ───────────────────────────────────────────────────────────────
data class VoiceState(
    val isListening: Boolean = false,
    val transcript: String = "",
    val confidence: Float = 0f,
    val isFinal: Boolean = false,
    val error: String? = null
)
