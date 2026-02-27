package com.cleaningos.presentation.features.checklist

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cleaningos.domain.model.Checklist
import com.cleaningos.domain.model.ChecklistStep
import com.cleaningos.presentation.components.*
import com.cleaningos.presentation.theme.*
import kotlinx.coroutines.flow.collectLatest

/** Thin screen — pure rendering */
class ChecklistScreen(private val checklist: Checklist) : Screen {

    @Composable
    override fun Content() {
        val viewModel: ChecklistViewModel = koinScreenModel()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        var showDoneDialog by remember { mutableStateOf(false) }

        LaunchedEffect(checklist) {
            viewModel.dispatch(ChecklistEvent.Load(checklist))
        }

        LaunchedEffect(Unit) {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is ChecklistEffect.NavigateBack        -> navigator.pop()
                    is ChecklistEffect.NavigateToDashboard -> navigator.popUntilRoot()
                    is ChecklistEffect.ShowCompletion      -> showDoneDialog = true
                    is ChecklistEffect.RequestCameraPermission -> { /* handle permission */ }
                }
            }
        }

        if (showDoneDialog) {
            CompletionDialog(
                done  = state.completedCount,
                total = state.steps.size,
                onDismiss = { showDoneDialog = false },
                onHome    = { viewModel.dispatch(ChecklistEvent.GoBack) }
            )
        }

        Scaffold(containerColor = OceanDeep) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                OceanTopBar(
                    title    = checklist.name,
                    subtitle = "${state.completedCount}/${state.steps.size} шагов"
                )

                // Progress bar
                ProgressSection(
                    progress = state.progressPercent,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )

                // Steps list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Warnings
                    state.checklist?.warnings?.forEach { warning ->
                        WarningGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Warning, null, tint = SemanticWarning)
                                Text(warning, color = SemanticWarning, fontSize = 13.sp)
                            }
                        }
                    }

                    // Steps
                    state.steps.forEachIndexed { index, step ->
                        StepItemCard(
                            step    = step,
                            index   = index,
                            onToggle = { viewModel.dispatch(ChecklistEvent.ToggleStep(index)) }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Bottom action bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FrostedGlass)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassButton(
                        text     = "Фото",
                        onClick  = { viewModel.dispatch(ChecklistEvent.TakePhoto) },
                        style    = ButtonStyle.Ghost,
                        modifier = Modifier.weight(0.3f)
                    )
                    GlassButton(
                        text     = "Закрыть объект",
                        onClick  = { viewModel.dispatch(ChecklistEvent.CloseObject) },
                        style    = ButtonStyle.Success,
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressSection(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue    = progress,
        animationSpec  = tween(400, easing = EaseInOutCubic),
        label          = "progress"
    )

    GlassCard(modifier = modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Text(
            text       = "${(progress * 100).toInt()}%",
            color      = CyanMint,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress         = { animatedProgress },
            modifier         = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).height(10.dp),
            color            = CyanMint,
            trackColor       = FrostedGlassDark,
            strokeCap        = StrokeCap.Round
        )
    }
}

@Composable
private fun StepItemCard(
    step: ChecklistStep,
    index: Int,
    onToggle: () -> Unit
) {
    GlassCard(
        modifier  = Modifier.fillMaxWidth(),
        glowColor = if (step.isDone) SemanticSuccess else CyanMint,
        glowAlpha = if (step.isDone) 0.40f else 0.22f,
        padding   = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked  = step.isDone,
                onCheckedChange = { onToggle() },
                colors   = CheckboxDefaults.colors(
                    checkedColor   = CyanMint,
                    uncheckedColor = TextDisabled,
                    checkmarkColor = OceanDeep
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                if (step.critical) {
                    Text(
                        text       = "ВАЖНО",
                        color      = SemanticError,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text           = "${index + 1}. ${step.text}",
                    color          = if (step.isDone) TextDisabled else TextPrimary,
                    fontSize       = 14.sp,
                    textDecoration = if (step.isDone) TextDecoration.LineThrough else TextDecoration.None
                )
                if (step.minutes > 0) {
                    Text("${step.minutes} мин", color = TextDisabled, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CompletionDialog(done: Int, total: Int, onDismiss: () -> Unit, onHome: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = FrostedGlass,
        title = {
            Text("Объект закрыт!", color = CyanMint, fontWeight = FontWeight.Bold)
        },
        text = {
            Text("$done / $total шагов выполнено.", color = TextPrimary)
        },
        confirmButton = {
            GlassButton("На главную", onClick = onHome, style = ButtonStyle.Success)
        },
        dismissButton = {
            GlassButton("Закрыть", onClick = onDismiss, style = ButtonStyle.Ghost)
        }
    )
}
