package com.saku.v2.ui.liquidglass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.saku.v2.ui.theme.SakuTheme

/**
 * Provides the ambient canvas [Backdrop] to descendant glass surfaces.
 * Note: [AuroraBackground] places [Modifier.layerBackdrop] on a sibling behind [content],
 * preventing any drawBackdrop capture cycle inside [content].
 */
val LocalCanvasBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun AuroraBackground(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = SakuTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.canvasGradientStart,
                                colors.canvasGradientMid,
                                colors.canvasGradientEnd
                            )
                        )
                    )

                    val w = size.width
                    val h = size.height

                    // Top-left Sage Mist pool
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.poolSage, Color.Transparent),
                            center = Offset(w * 0.16f, h * 0.14f),
                            radius = w * 0.72f
                        ),
                        center = Offset(w * 0.16f, h * 0.14f),
                        radius = w * 0.72f
                    )

                    // Mid-right Warm Limestone pool
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.poolLimestone, Color.Transparent),
                            center = Offset(w * 0.88f, h * 0.42f),
                            radius = w * 0.68f
                        ),
                        center = Offset(w * 0.88f, h * 0.42f),
                        radius = w * 0.68f
                    )

                    // Lower-left Muted Slate Sky pool
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.poolSlate, Color.Transparent),
                            center = Offset(w * 0.24f, h * 0.82f),
                            radius = w * 0.75f
                        ),
                        center = Offset(w * 0.24f, h * 0.82f),
                        radius = w * 0.75f
                    )
                }
        )

        CompositionLocalProvider(LocalCanvasBackdrop provides backdrop) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}
