package com.cleaningos.presentation.features.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cleaningos.domain.model.Checklist
import com.cleaningos.domain.model.SearchResult
import com.cleaningos.presentation.components.*
import com.cleaningos.presentation.features.checklist.ChecklistScreen
import com.cleaningos.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest

/**
 * DashboardScreen — "Thin Screen" implementation.
 *
 * This Composable ONLY:
 *   ✅ Reads state via collectAsState()
 *   ✅ Dispatches events via viewModel.dispatch()
 *   ❌ Contains ZERO business logic
 */
class DashboardScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel: DashboardViewModel = koinScreenModel()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val snackbarHostState = remember { SnackbarHostState() }

        // Consume one-time effects
        LaunchedEffect(Unit) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is DashboardEffect.OpenChecklistScreen ->
                        navigator.push(ChecklistScreen(effect.checklist))
                    is DashboardEffect.ShowSnackbar ->
                        snackbarHostState.showSnackbar(effect.message)
                    is DashboardEffect.ShowVoiceDialog  -> { /* trigger voice dialog */ }
                    is DashboardEffect.NavigateTo       -> { /* handle named routes */ }
                    is DashboardEffect.RequestPermission -> { /* request Android permission */ }
                }
            }
        }

        Scaffold(
            containerColor = OceanDeep,
            snackbarHost   = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            DashboardContent(
                state    = state,
                onEvent  = viewModel::dispatch,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ── Content: pure rendering, reads state only ────────────────────────────────

@Composable
private fun DashboardContent(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {

        // ── Header ───────────────────────────────────────────────────────────
        OceanTopBar(
            title    = "Cleaning OS v2.6",
            subtitle = "Умный помощник · Хабаровск"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── EDI Bar ───────────────────────────────────────────────────────
            EdiBar(
                edi = EdiState(
                    E = state.edi.E,
                    D = state.edi.D,
                    I = state.edi.I
                )
            )

            // ── Search Input Card ─────────────────────────────────────────────
            SearchInputCard(
                query   = state.query,
                onQuery = { onEvent(DashboardEvent.QueryChanged(it)) },
                onSearch = { onEvent(DashboardEvent.SearchClicked) },
                onVoice  = { onEvent(DashboardEvent.VoiceClicked) }
            )

            // ── Quick Actions Grid ────────────────────────────────────────────
            QuickActionsGrid(
                actions = state.quickActions,
                onClick = { onEvent(DashboardEvent.QuickActionClicked(it)) }
            )

            // ── Search Button ─────────────────────────────────────────────────
            GlassButton(
                text     = "Найти",
                onClick  = { onEvent(DashboardEvent.SearchClicked) },
                style    = ButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Loading ───────────────────────────────────────────────────────
            AnimatedVisibility(visible = state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color            = CyanMint,
                    trackColor       = FrostedGlass
                )
            }

            // ── Error ─────────────────────────────────────────────────────────
            state.errorMessage?.let { error ->
                DangerGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, "error", tint = SemanticError)
                        Text(error, color = SemanticError, fontSize = 14.sp)
                    }
                }
            }

            // ── Search Results ────────────────────────────────────────────────
            state.searchResult?.let { result ->
                SearchResultsSection(result = result, onEvent = onEvent)
            }

            // ── Music Player ──────────────────────────────────────────────────
            state.musicState.currentTrack?.let { track ->
                MiniPlayerCard(
                    musicState = state.musicState,
                    onPlayPause = { onEvent(DashboardEvent.PlayPauseClicked) },
                    onNext      = { onEvent(DashboardEvent.NextTrackClicked) },
                    onPrev      = { onEvent(DashboardEvent.PrevTrackClicked) }
                )
            }

            Spacer(Modifier.height(80.dp)) // nav bar clearance
        }
    }
}

// ── Search Input Card ─────────────────────────────────────────────────────────

@Composable
private fun SearchInputCard(
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onVoice: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding  = PaddingValues(16.dp)
    ) {
        Text(
            text       = "Как дела? Что убираешь?",
            color      = TextSecondary,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value           = query,
                onValueChange   = onQuery,
                modifier        = Modifier.weight(1f),
                placeholder     = { Text("кухня, клиент едет, сил нет...", color = TextDisabled) },
                shape           = RoundedCornerShape(18.dp),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = CyanMint,
                    unfocusedBorderColor = GlowCyan20,
                    cursorColor          = CyanMint,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    unfocusedContainerColor = FrostedGlassDark,
                    focusedContainerColor   = FrostedGlassDark
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                maxLines        = 3,
                minLines        = 2
            )

            // Voice button
            IconButton(
                onClick  = onVoice,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(FrostedGlass)
                    .border(1.5.dp, GlowCyan35, RoundedCornerShape(18.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Голосовой ввод",
                    tint = CyanMint
                )
            }
        }
    }
}

// ── Quick Actions 2×2 Grid ────────────────────────────────────────────────────

@Composable
private fun QuickActionsGrid(
    actions: List<QuickAction>,
    onClick: (QuickAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        actions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { action ->
                    GlassButton(
                        text     = action.label,
                        onClick  = { onClick(action) },
                        style    = when (action.style) {
                            QuickAction.ActionStyle.Danger  -> ButtonStyle.Danger
                            QuickAction.ActionStyle.Purple  -> ButtonStyle.Purple
                            QuickAction.ActionStyle.Primary -> ButtonStyle.Primary
                            QuickAction.ActionStyle.Warning -> ButtonStyle.Warning
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ── Search Results ────────────────────────────────────────────────────────────

@Composable
private fun SearchResultsSection(
    result: SearchResult,
    onEvent: (DashboardEvent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Safety alert
        if (result.safetyAlert) {
            DangerGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Warning, null, tint = SemanticError, modifier = Modifier.size(28.dp))
                    Column {
                        Text("ОПАСНОСТЬ", color = SemanticError, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(result.alertText, color = SemanticError, fontSize = 14.sp)
                    }
                }
            }
            return
        }

        // Chemical warning
        result.chemicalWarning?.let { cw ->
            val color = if (cw.isDeadly) SemanticError else SemanticWarning
            WarningGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = (if (cw.isDeadly) "СМЕРТЕЛЬНО: " else "ОСТОРОЖНО: ") + cw.reaction,
                    color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(cw.action, color = TextSecondary, fontSize = 13.sp)
            }
        }

        // Support phrase
        result.supportPhrase?.let { phrase ->
            GlassCard(
                modifier  = Modifier.fillMaxWidth(),
                glowColor = when (result.edi.E) {
                    1    -> EnergyLv1
                    2    -> EnergyLv2
                    3    -> EnergyLv3
                    4    -> EnergyLv4
                    else -> EnergyLv5
                }
            ) {
                Text(phrase, color = TextPrimary, fontSize = 15.sp)
            }
        }

        // Checklist card
        result.checklist?.let { cl ->
            GlassCard(
                modifier  = Modifier.fillMaxWidth(),
                glowColor = CyanMint,
                glowAlpha = 0.45f
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Checklist, null, tint = CyanMint, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(cl.name, color = CyanMint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("~${cl.estimatedMinutes} мин", color = CyanMint, fontSize = 13.sp)
                    Text("${cl.steps.size} шагов", color = SkyBlue, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    text     = "Открыть чеклист",
                    onClick  = { onEvent(DashboardEvent.OpenChecklist(cl)) },
                    style    = ButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Stain info
        result.stainInfo?.let { stain ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(stain.name + " — как убрать", color = CyanMint, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                stain.steps.take(4).forEach { step ->
                    Text("- $step", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(2.dp))
                }
            }
        }

        // KB results
        if (result.kbResults.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Из баз знаний:", color = CyanMint, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                result.kbResults.take(2).forEach { kb ->
                    Text(kb.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        text     = kb.snippet.take(120) + "...",
                        color    = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Mini Music Player ─────────────────────────────────────────────────────────

@Composable
private fun MiniPlayerCard(
    musicState: com.cleaningos.domain.model.MusicState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val track = musicState.currentTrack ?: return
    GlassCard(
        modifier  = Modifier.fillMaxWidth(),
        glowColor = SkyBlue,
        padding   = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title,  color = TextPrimary,   fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Text(track.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            IconButton(onClick = onPrev) {
                Icon(Icons.Rounded.SkipPrevious, null, tint = CyanMint)
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (musicState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = CyanMint,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.SkipNext, null, tint = CyanMint)
            }
        }
    }
}
