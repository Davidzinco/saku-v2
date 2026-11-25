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

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.saku.v2.ui.theme.SakuTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val colors = SakuTheme.colors
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val shape = remember { Capsule() }
    val useBackdrop = backdrop != null && isRenderEffectSupported()
    val useLens = isRuntimeShaderSupported()

    val surfaceModifier = if (useBackdrop) {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(4f.dp.toPx())
                if (useLens) {
                    lens(10f.dp.toPx(), 20f.dp.toPx())
                }
            },
            highlight = {
                Highlight(
                    width = 0.6.dp,
                    alpha = if (colors.isDark) 0.25f else 0.7f
                )
            },
            shadow = {
                Shadow(
                    radius = 12.dp,
                    color = Color.Black.copy(alpha = if (colors.isDark) 0.22f else 0.06f)
                )
            },
            layerBlock = if (isInteractive && enabled) {
                {
                    val width = size.width
                    val height = size.height
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                    val maxOffset = size.minDimension
                    val initialDerivative = 0.05f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                    val maxDragScale = 4f.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX = scale +
                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                        (width / height).fastCoerceAtMost(1f)
                    scaleY = scale +
                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                        (height / width).fastCoerceAtMost(1f)
                }
            } else {
                null
            },
            onDrawSurface = {
                if (tint.isSpecified) {
                    drawRect(tint, blendMode = BlendMode.Hue)
                    drawRect(tint.copy(alpha = 0.78f))
                }
                if (surfaceColor.isSpecified) {
                    drawRect(surfaceColor)
                } else if (!tint.isSpecified) {
                    drawRect(if (useLens) colors.glassSurface else colors.glassSurfaceFallback)
                }
            }
        )
    } else {
        val fallbackFill = when {
            tint.isSpecified -> tint
            surfaceColor.isSpecified -> surfaceColor
            else -> colors.glassSurfaceFallback
        }
        Modifier
            .clip(shape)
            .background(fallbackFill)
            .border(
                width = 0.75.dp,
                brush = Brush.verticalGradient(listOf(colors.glassTopRim, colors.glassBottomHairline)),
                shape = shape
            )
    }

    Row(
        modifier
            .alpha(if (enabled) 1f else 0.48f)
            .then(surfaceModifier)
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = if (isInteractive && useBackdrop) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick
            )
            .then(
                if (isInteractive && enabled && useBackdrop) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(48.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
