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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.saku.v2.ui.theme.SakuTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    val accentColor = colors.primary
    val trackColor = if (colors.isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color(0xFF1A1D1F).copy(alpha = 0.16f)
    }

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }

    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.45f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onSelect(fraction == 1f)
                    didDrag = false
                } else {
                    fraction = if (selected()) 0f else 1f
                    onSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction = if (isLtr) {
                    (fraction + delta).fastCoerceIn(0f, 1f)
                } else {
                    (fraction - delta).fastCoerceIn(0f, 1f)
                }
            }
        )
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { value ->
                dampedDragAnimation.updateValue(value)
            }
    }

    LaunchedEffect(selected) {
        snapshotFlow { selected() }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    val capsuleShape = remember { Capsule() }
    val trackBackdrop = rememberLayerBackdrop()
    val useRenderEffect = isRenderEffectSupported()
    val useLens = isRuntimeShaderSupported()
    val resolvedBackdrop = backdrop ?: emptyBackdrop()

    Box(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(capsuleShape)
                .drawBehind {
                    val value = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, value))
                }
                .size(64.dp, 28.dp)
        )

        val thumbSurfaceModifier = if (useRenderEffect) {
            Modifier.drawBackdrop(
                backdrop = rememberCombinedBackdrop(
                    resolvedBackdrop,
                    rememberBackdrop(trackBackdrop) { drawBackdrop ->
                        val progress = dampedDragAnimation.pressProgress
                        val sx = lerp(2f / 3f, 0.75f, progress)
                        val sy = lerp(0f, 0.75f, progress)
                        scale(sx, sy) {
                            drawBackdrop()
                        }
                    }
                ),
                shape = { capsuleShape },
                effects = {
                    val progress = dampedDragAnimation.pressProgress
                    blur(8.dp.toPx() * (1f - progress))
                    if (useLens) {
                        lens(
                            5.dp.toPx() * progress,
                            10.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    }
                },
                highlight = {
                    val progress = dampedDragAnimation.pressProgress
                    Highlight.Ambient.copy(
                        width = Highlight.Ambient.width / 1.5f,
                        blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                        alpha = progress
                    )
                },
                shadow = {
                    Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.08f))
                },
                innerShadow = {
                    val progress = dampedDragAnimation.pressProgress
                    InnerShadow(radius = 4.dp * progress, alpha = progress)
                },
                layerBlock = {
                    scaleX = dampedDragAnimation.scaleX
                    scaleY = dampedDragAnimation.scaleY
                    val velocity = dampedDragAnimation.velocity / 50f
                    scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                    scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                },
                onDrawSurface = {
                    val progress = dampedDragAnimation.pressProgress
                    val thumbColor = if (colors.isDark) Color(0xFFF1EFEA) else Color.White
                    drawRect(thumbColor.copy(alpha = 1f - progress))
                }
            )
        } else {
            Modifier
                .clip(capsuleShape)
                .background(if (colors.isDark) Color(0xFFF1EFEA) else Color.White)
                .border(0.5.dp, Color.Black.copy(alpha = 0.08f), capsuleShape)
        }

        Box(
            Modifier
                .graphicsLayer {
                    val value = dampedDragAnimation.value
                    val padding = 2.dp.toPx()
                    translationX = if (isLtr) {
                        lerp(padding, padding + dragWidth, value)
                    } else {
                        lerp(-padding, -(padding + dragWidth), value)
                    }
                }
                .semantics { role = Role.Switch }
                .then(dampedDragAnimation.modifier)
                .then(thumbSurfaceModifier)
                .size(40.dp, 24.dp)
        )
    }
}
