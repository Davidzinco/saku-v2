/*
 * Copyright 2025 Kyant
 * Adapted for Saku V2 (Mineral Alabaster / Smoked Obsidian editorial palette & API fallbacks).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package com.saku.v2.ui.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.saku.v2.ui.theme.SakuTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

internal val LocalLiquidBottomTabScale = staticCompositionLocalOf { { 1f } }

@Composable
fun RowScope.LiquidBottomTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    Column(
        modifier
            .clip(Capsule())
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = SakuTheme.colors
    val accentColor = colors.primary
    val useRenderEffect = isRenderEffectSupported()
    val useLens = isRuntimeShaderSupported()
    val containerColor = if (useLens) colors.glassSurface else colors.glassSurfaceFallback
    val capsuleShape = remember { Capsule() }

    val tabsBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount.coerceAtLeast(1)
        }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val maxWidth = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val fraction = (offsetAnimation.value / maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        val latestSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
        val latestOnTabSelected by rememberUpdatedState(onTabSelected)
        val latestTabWidth by rememberUpdatedState(tabWidth)
        val latestIsLtr by rememberUpdatedState(isLtr)

        val dampedDragAnimation = remember(animationScope, tabsCount) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = latestSelectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).coerceAtLeast(0).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 76f / 56f,
                onDragStarted = {},
                onDragStopped = {},
                onDrag = { _, _ -> }
            )
        }

        // Sync external tab selection changes (taps on tabs or HorizontalPager swipes) to the glass pill
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { latestSelectedTabIndex() }
                .distinctUntilChanged()
                .collectLatest { index ->
                    val clamped = index.fastCoerceIn(0, (tabsCount - 1).coerceAtLeast(0))
                    if (abs(dampedDragAnimation.targetValue - clamped.toFloat()) > 0.01f) {
                        dampedDragAnimation.animateToValue(clamped.toFloat())
                    }
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, _ ->
                    Offset(
                        if (latestIsLtr) (dampedDragAnimation.value + 0.5f) * latestTabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * latestTabWidth + panelOffset,
                        size.height / 2f
                    )
                }
            )
        }

        // Intercept horizontal drag gestures on Initial pass so the glass pill can be scrubbed
        // across the dock while unconsumed taps pass cleanly through to LiquidBottomTab.clickable.
        val dockScrubModifier = Modifier.pointerInput(tabsCount) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial
                )
                val activePointer = down.id
                var dragStarted = false
                var finishedCleanly = false
                val slop = viewConfiguration.touchSlop
                var totalDx = 0f
                var totalDy = 0f

                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.fastFirstOrNull { it.id == activePointer } ?: break

                        if (!change.pressed) {
                            if (dragStarted) {
                                change.consume()
                                val maxIdx = (tabsCount - 1).coerceAtLeast(0)
                                val targetIndex = dampedDragAnimation.targetValue
                                    .fastRoundToInt()
                                    .fastCoerceIn(0, maxIdx)
                                dampedDragAnimation.animateToValue(targetIndex.toFloat())
                                interactiveHighlight.endHighlight()
                                animationScope.launch {
                                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                                }
                                if (targetIndex != latestSelectedTabIndex()) {
                                    latestOnTabSelected(targetIndex)
                                }
                                finishedCleanly = true
                            }
                            break
                        }

                        val delta = change.position - change.previousPosition
                        if (!dragStarted) {
                            totalDx += delta.x
                            totalDy += delta.y
                            if (abs(totalDx) > slop && abs(totalDx) > abs(totalDy)) {
                                dragStarted = true
                                change.consume()
                                dampedDragAnimation.press()
                                interactiveHighlight.startHighlight(down.position)
                                val maxVal = (tabsCount - 1).coerceAtLeast(0).toFloat()
                                val step = totalDx / latestTabWidth.coerceAtLeast(1f) *
                                    if (latestIsLtr) 1f else -1f
                                dampedDragAnimation.updateValue(
                                    (dampedDragAnimation.targetValue + step).fastCoerceIn(0f, maxVal)
                                )
                                animationScope.launch {
                                    offsetAnimation.snapTo(offsetAnimation.value + totalDx)
                                }
                            }
                        } else {
                            change.consume()
                            val maxVal = (tabsCount - 1).coerceAtLeast(0).toFloat()
                            val step = delta.x / latestTabWidth.coerceAtLeast(1f) *
                                if (latestIsLtr) 1f else -1f
                            dampedDragAnimation.updateValue(
                                (dampedDragAnimation.targetValue + step).fastCoerceIn(0f, maxVal)
                            )
                            interactiveHighlight.updateHighlight(change.position)
                            animationScope.launch {
                                offsetAnimation.snapTo(offsetAnimation.value + delta.x)
                            }
                        }
                    }
                } finally {
                    if (dragStarted && !finishedCleanly) {
                        val maxIdx = (tabsCount - 1).coerceAtLeast(0)
                        val targetIndex = dampedDragAnimation.targetValue
                            .fastRoundToInt()
                            .fastCoerceIn(0, maxIdx)
                        dampedDragAnimation.animateToValue(targetIndex.toFloat())
                        interactiveHighlight.endHighlight()
                        animationScope.launch {
                            offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                        }
                    }
                }
            }
        }

        val dockSurfaceModifier = if (useRenderEffect) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { capsuleShape },
                effects = {
                    vibrancy()
                    blur(10.dp.toPx())
                    if (useLens) {
                        lens(20.dp.toPx(), 24.dp.toPx())
                    }
                },
                highlight = {
                    Highlight(
                        width = 0.75.dp,
                        alpha = if (colors.isDark) 0.25f else 0.75f
                    )
                },
                shadow = {
                    Shadow(
                        radius = 20.dp,
                        color = Color.Black.copy(alpha = if (colors.isDark) 0.34f else 0.10f)
                    )
                },
                layerBlock = {
                    val progress = dampedDragAnimation.pressProgress
                    val scale = lerp(1f, 1f + 12.dp.toPx() / size.width.coerceAtLeast(1f), progress)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = { drawRect(containerColor) }
            )
        } else {
            Modifier
                .clip(capsuleShape)
                .background(containerColor)
                .border(
                    width = 0.8.dp,
                    brush = Brush.verticalGradient(listOf(colors.glassTopRim, colors.glassBottomHairline)),
                    shape = capsuleShape
                )
        }

        Row(
            Modifier
                .then(dockScrubModifier)
                .graphicsLayer { translationX = panelOffset }
                .then(dockSurfaceModifier)
                .then(interactiveHighlight.modifier)
                .height(64.dp)
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        if (useRenderEffect) {
            CompositionLocalProvider(
                LocalLiquidBottomTabScale provides {
                    lerp(1f, 1.18f, dampedDragAnimation.pressProgress)
                }
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { capsuleShape },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                vibrancy()
                                blur(10.dp.toPx())
                                if (useLens) {
                                    lens(20.dp.toPx() * progress, 24.dp.toPx() * progress)
                                }
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = progress)
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .then(interactiveHighlight.modifier)
                        .height(56.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        val pillModifier = if (useRenderEffect) {
            Modifier.drawBackdrop(
                backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                shape = { capsuleShape },
                effects = {
                    val progress = dampedDragAnimation.pressProgress
                    if (useLens) {
                        lens(
                            10.dp.toPx() * progress,
                            14.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    }
                },
                highlight = {
                    val progress = dampedDragAnimation.pressProgress
                    Highlight.Default.copy(alpha = 0.25f + 0.75f * progress)
                },
                shadow = {
                    val progress = dampedDragAnimation.pressProgress
                    Shadow(alpha = progress)
                },
                innerShadow = {
                    val progress = dampedDragAnimation.pressProgress
                    InnerShadow(radius = 8.dp * progress, alpha = progress)
                },
                layerBlock = {
                    scaleX = dampedDragAnimation.scaleX
                    scaleY = dampedDragAnimation.scaleY
                    val velocity = dampedDragAnimation.velocity / 10f
                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                },
                onDrawSurface = {
                    val progress = dampedDragAnimation.pressProgress
                    drawRect(
                        colors.primary.copy(alpha = if (colors.isDark) 0.16f else 0.08f),
                        alpha = 1f - progress
                    )
                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                }
            )
        } else {
            Modifier
                .clip(capsuleShape)
                .background(colors.primary.copy(alpha = if (colors.isDark) 0.20f else 0.12f))
                .border(0.75.dp, colors.primary.copy(alpha = 0.22f), capsuleShape)
        }

        // Visual-only lens pill overlay (no pointerInput so tabs underneath stay tappable)
        Box(
            Modifier
                .padding(horizontal = 4.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(pillModifier)
                .height(56.dp)
                .fillMaxWidth(1f / tabsCount.coerceAtLeast(1))
        )
    }
}
