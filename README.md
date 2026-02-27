# Cleaning OS — KMP Architecture

## Stack
| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.0 |
| UI | Compose Multiplatform 1.7.0 |
| Navigation | Voyager 1.1.0 (Tab + Screen) |
| DI | Koin 3.5.0 |
| Architecture | MVI (BaseViewModel + StateFlow/SharedFlow) |
| Media | Media3 / ExoPlayer 1.4.0 |
| DB | SQLDelight 2.0.2 |

## Architecture: MVI "Thin Screen" Protocol

```
User Action
    │
    ▼
[Composable] ──dispatch(Event)──► [ViewModel]
    │                                   │
    │◄──────collect(State)──────────────┤
    │                                   │
    │◄──────collect(Effect)─────────────┤
    │         (one-shot)           (side-effects)
    │                                   │
    │                              [Repository]
    │                                   │
    │                         [DataSource / Service]
```

**Rule**: Composables contain ZERO logic. They only:
1. Read `state` via `collectAsState()`
2. Dispatch `events` via `viewModel.dispatch(Event)`
3. React to `effects` via `LaunchedEffect`

## Module Structure

```
composeApp/src/
├── commonMain/
│   └── kotlin/com/cleaningos/
│       ├── core/
│       │   ├── mvi/
│       │   │   └── BaseViewModel.kt        ← StateFlow + SharedFlow MVI core
│       │   ├── di/
│       │   │   └── CommonModule.kt         ← Koin commonMain module
│       │   └── utils/
│       │       └── Platform.kt             ← expect declarations
│       ├── domain/
│       │   ├── model/
│       │   │   └── Models.kt               ← AudioTrack, Checklist, EdiScore...
│       │   └── repository/
│       │       └── Repositories.kt         ← interfaces: Search, Media, Voice, Player
│       ├── data/
│       │   └── repository/
│       │       └── SearchRepositoryImpl.kt ← EDI parser, checklist selector, KB search
│       └── presentation/
│           ├── theme/
│           │   ├── Color.kt                ← Dark Ocean palette + glow colors
│           │   ├── Theme.kt                ← MaterialTheme (always dark)
│           │   └── Type.kt                 ← Typography
│           ├── components/
│           │   ├── GlassCard.kt            ← 25dp radius, cyan glow border
│           │   ├── GlassButton.kt          ← Liquid button with press animation
│           │   ├── OceanTopBar.kt          ← Frosted glass header + glow orb
│           │   └── EdiBar.kt               ← Animated E/D/I indicator pills
│           ├── navigation/
│           │   └── AppNavigation.kt        ← TabNavigator (5 tabs)
│           └── features/
│               ├── dashboard/
│               │   ├── DashboardContract.kt  ← State / Event / Effect types
│               │   ├── DashboardViewModel.kt ← All business logic
│               │   ├── DashboardScreen.kt    ← Thin Screen: rendering only
│               │   └── MusicViewModel.kt     ← Music MVI (scan, play, pause)
│               └── checklist/
│                   ├── ChecklistContract.kt
│                   ├── ChecklistViewModel.kt ← Step toggling, progress calc
│                   └── ChecklistScreen.kt    ← Progress bar, step list
│
├── androidMain/
│   └── kotlin/com/cleaningos/
│       ├── CleaningOSApp.kt               ← Application + Koin init
│       ├── MainActivity.kt                ← setContent { AppNavigation() }
│       ├── core/
│       │   ├── di/AndroidModule.kt        ← Android-specific Koin bindings
│       │   └── platform/
│       │       └── AppContextHolder.kt    ← Singleton Context
│       ├── media/
│       │   ├── repository/
│       │   │   └── MediaRepositoryImpl.kt ← MediaStore scanner + ContentObserver
│       │   ├── player/
│       │   │   └── AndroidAudioPlayerController.kt ← Bridges domain ↔ service
│       │   └── service/
│       │       └── AudioPlayerService.kt  ← ExoPlayer + MediaSession foreground service
│       └── voice/
│           ├── repository/
│           │   └── VoiceRepositoryImpl.kt ← Binds to STT service
│           └── service/
│               └── SpeechRecognitionService.kt ← STT + audio focus ducking
│
└── iosMain/
    └── kotlin/com/cleaningos/
        ├── core/platform/Platform.ios.kt  ← actual implementations
        ├── media/MediaRepositoryIos.kt    ← stub (MPMediaLibrary TODO)
        └── voice/VoiceRepositoryIos.kt    ← stub (SFSpeechRecognizer TODO)
```

## Audio Focus Ducking Flow

```
[User says voice command]
        │
        ▼
SpeechRecognitionService.startRecognition()
        │
        ▼
AudioManager.requestAudioFocus(GAIN_TRANSIENT_MAY_DUCK)
        │
        ├──► System ducks music automatically (ExoPlayer handles AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
        │    OR manually: AudioPlayerService.setVolume(0.3f)
        │
        ▼
SpeechRecognizer.startListening() → onResults()
        │
        ▼
VoiceRepository emits VoiceState(isFinal=true, transcript="...")
        │
        ▼
DashboardViewModel.dispatch(VoiceResult(transcript))
        │
        ▼
performSearch(transcript) → SearchResult → setState { copy(searchResult=...) }
        │
        ▼
AudioManager.abandonAudioFocus() → music restores full volume
```

## Key Design Decisions

1. **BaseViewModel extends ScreenModel** (Voyager) — lifecycle tied to screen, not Activity.
2. **Effects via Channel** (not SharedFlow) — guarantees delivery even with no collectors.
3. **Service ↔ ViewModel via StateFlow** — no direct references; services expose Flow, ViewModels observe.
4. **SearchRepositoryImpl in commonMain** — all EDI/checklist logic runs on both platforms (no Android APIs).
5. **GlassCard** uses `shadow()` for glow effect — pure Compose, no Canvas needed.
