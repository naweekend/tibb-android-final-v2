package com.example.ui.components.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AppLocale
import com.example.data.model.WorkoutExerciseSlot
import com.example.ui.components.TibbStatusBadge
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.urduTextStyle

private val LightSilhouetteColor = Color(0xFF141517)
private val DarkSilhouetteColor = Color(0xFFF5F6F7)
private val LightStageColor = Color(0xFFF3F4F6)
private val DarkStageColor = Color(0xFF181B20)
private val LightPropColor = Color(0xFFD2D6DE)
private val DarkPropColor = Color(0xFF343943)
private val LightFloorColor = Color(0xFFE1E4EC)
private val DarkFloorColor = Color(0xFF282D36)

private val SpeedSteps = listOf(0.75f, 1.0f, 1.5f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutExerciseCard(
    index: Int,
    exercise: WorkoutExerciseSlot,
    gender: SilhouetteGender,
    locale: AppLocale,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalTibbColors.current
    val isUr = locale == AppLocale.UR
    val movements = remember(exercise.name_en) { resolveExerciseMovements(exercise) }

    var selectedMovementIdx by remember(exercise.name_en) { mutableIntStateOf(0) }
    val activeSpec = movements[selectedMovementIdx.coerceIn(0, movements.lastIndex)]

    var isPlaying by remember(exercise.name_en) { mutableStateOf(true) }
    var speedIdx by remember { mutableIntStateOf(1) } // 1.0x default
    val speedMultiplier = SpeedSteps[speedIdx]
    var phase by remember(exercise.name_en, activeSpec.type) { mutableFloatStateOf(0f) }

    // Lifecycle awareness: pause animation frame loop when screen/app is paused
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleActive by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> isLifecycleActive = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isLifecycleActive = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Only animate when expanded, playing, and app is active (off-screen/collapsed items use 0% animation CPU)
    LaunchedEffect(isExpanded, isPlaying, speedMultiplier, activeSpec.type, isLifecycleActive) {
        if (!isExpanded || !isPlaying || !isLifecycleActive) return@LaunchedEffect
        var lastFrameMs = withFrameMillis { it }
        while (true) {
            val nowMs = withFrameMillis { it }
            val dtMs = (nowMs - lastFrameMs).coerceIn(0L, 100L)
            lastFrameMs = nowMs
            val deltaPhase = (dtMs.toFloat() / activeSpec.type.cycleDurationMs.toFloat()) * speedMultiplier
            phase = (phase + deltaPhase) % 1f
        }
    }

    val silhouetteColor = if (c.isDark) DarkSilhouetteColor else LightSilhouetteColor
    val stageColor = if (c.isDark) DarkStageColor else LightStageColor
    val propColor = if (c.isDark) DarkPropColor else LightPropColor
    val floorColor = if (c.isDark) DarkFloorColor else LightFloorColor

    val headerPool = remember { SilhouettePathPool() }
    val stagePool = remember { SilhouettePathPool() }

    val exerciseTitle = if (isUr) exercise.name_ur else exercise.name_en
    val stageDescription = if (isUr) {
        "$exerciseTitle کی مکمل جسمانی ورزش کی اینیمیشن"
    } else {
        "Animated full-body demonstration of $exerciseTitle"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceMuted.copy(alpha = if (c.isDark) 0.55f else 0.72f))
            .border(
                width = 1.dp,
                color = if (isExpanded) c.accent.copy(alpha = 0.42f) else c.border,
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        // Header row (always visible, 48dp+ touch target)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .clickable(role = Role.Button, onClick = onToggleExpand)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("workout_exercise_card_$index"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(c.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${index + 1}",
                    style = TabularNumberStyle.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    ),
                    color = c.accent,
                )
            }

            // Compact mid-pose static silhouette preview thumbnail
            val previewPose = remember(activeSpec.type, gender) {
                evaluateExercisePose(activeSpec.type, phase = 0.30f, gender = gender)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(stageColor)
                    .border(1.dp, c.border.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSolidHumanSilhouette(
                        pose = previewPose,
                        gender = gender,
                        silhouetteColor = silhouetteColor,
                        propColor = propColor,
                        floorColor = floorColor,
                        paths = headerPool,
                        isThumbnail = true,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = exerciseTitle,
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.titleSmall) else MaterialTheme.typography.titleSmall,
                    color = c.ink,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = exercise.durationOrReps,
                        style = TabularNumberStyle.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        ),
                        color = c.accent,
                    )
                    TibbStatusBadge(
                        text = exercise.intensity,
                        color = c.accent,
                        softBg = c.accentSoft,
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (isExpanded) "Collapse exercise demonstration" else "Show exercise demonstration",
                tint = c.muted,
                modifier = Modifier.size(22.dp),
            )
        }

        // Expanded Demonstration Stage + Playback Controls + Instructions
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Clean, high-contrast demonstration stage (no gradients or distracting textures)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(228.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(stageColor)
                        .border(1.dp, c.border, RoundedCornerShape(16.dp))
                        .semantics { contentDescription = stageDescription }
                        .testTag("workout_animation_stage_$index"),
                ) {
                    val livePose = evaluateExercisePose(activeSpec.type, phase, gender)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawSolidHumanSilhouette(
                            pose = livePose,
                            gender = gender,
                            silhouetteColor = silhouetteColor,
                            propColor = propColor,
                            floorColor = floorColor,
                            paths = stagePool,
                            isThumbnail = false,
                        )
                    }

                    if (activeSpec.type.isStaticHold) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(c.surface.copy(alpha = 0.88f))
                                .border(1.dp, c.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = if (isUr) "پوزیشن برقرار رکھیں" else "Hold Posture",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.ink,
                            )
                        }
                    }
                }

                // Sub-movement selector pills when an exercise slot combines multiple distinct exercises
                if (movements.size > 1) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        movements.forEachIndexed { mIdx, spec ->
                            val selected = mIdx == selectedMovementIdx
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 40.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) c.ctaBg else c.surface)
                                    .border(1.dp, if (selected) c.ctaBg else c.border, RoundedCornerShape(999.dp))
                                    .clickable(role = Role.Tab) {
                                        selectedMovementIdx = mIdx
                                        phase = 0f
                                        isPlaying = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("workout_movement_pill_${index}_$mIdx"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (isUr) spec.labelUr else spec.labelEn,
                                    style = if (isUr) urduTextStyle(MaterialTheme.typography.labelSmall) else MaterialTheme.typography.labelSmall,
                                    color = if (selected) c.ctaInk else c.ink,
                                )
                            }
                        }
                    }
                }

                // Minimalist Playback Controls (48dp touch targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Play / Pause
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.ctaBg)
                            .clickable(role = Role.Button) { isPlaying = !isPlaying }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("workout_anim_play_pause_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause demonstration" else "Play demonstration",
                            tint = c.ctaInk,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isPlaying) (if (isUr) "روکیں" else "Pause") else (if (isUr) "چلائیں" else "Play"),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.ctaInk,
                        )
                    }

                    // Replay
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.surface)
                            .border(1.dp, c.border, RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button) {
                                phase = 0f
                                isPlaying = true
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("workout_anim_replay_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = "Replay demonstration",
                            tint = c.ink,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isUr) "دوبارہ" else "Replay",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.ink,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Speed Toggle (0.75x / 1x / 1.5x)
                    val speedLabel = when (speedMultiplier) {
                        0.75f -> "0.75x"
                        1.5f -> "1.5x"
                        else -> "1x"
                    }
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.surface)
                            .border(1.dp, c.border, RoundedCornerShape(999.dp))
                            .clickable(role = Role.Button) {
                                speedIdx = (speedIdx + 1) % SpeedSteps.size
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("workout_anim_speed_$index"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "Change animation speed",
                            tint = c.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = speedLabel,
                            style = TabularNumberStyle.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                            ),
                            color = c.ink,
                        )
                    }
                }

                // Clear instructions & form guidance below controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isUr) "طریقہ کار اور احتیاط" else "Form & Guidance",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = c.ink,
                    )
                    Text(
                        text = exercise.tip_en,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
            }
        }
    }
}
