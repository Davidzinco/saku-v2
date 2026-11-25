package com.saku.v2.ui.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.Shadow
import com.saku.v2.ui.theme.PlusJakartaSansFontFamily
import com.saku.v2.ui.theme.SakuTheme
import kotlinx.coroutines.launch

@Composable
fun LiquidGlassCard(
    backdrop: Backdrop? = LocalCanvasBackdrop.current,
    shape: Shape = RoundedCornerShape(24.dp),
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SakuTheme.colors
    val useLens = isRuntimeShaderSupported()
    val resolvedTint = tintColor ?: if (useLens) colors.glassSurface else colors.glassSurfaceFallback

    val rimBrush = Brush.verticalGradient(
        colors = listOf(colors.glassTopRim, colors.glassBottomHairline)
    )

    val glassModifier = if (backdrop != null && isRenderEffectSupported()) {
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(16.dp.toPx())
                    if (useLens) {
                        lens(10.dp.toPx(), 20.dp.toPx())
                    }
                },
                highlight = {
                    Highlight(
                        width = 0.75.dp,
                        alpha = if (colors.isDark) 0.24f else 0.70f
                    )
                },
                shadow = {
                    Shadow(
                        radius = 18.dp,
                        color = Color.Black.copy(alpha = if (colors.isDark) 0.26f else 0.06f)
                    )
                },
                onDrawSurface = { drawRect(resolvedTint) }
            )
            .border(width = 0.75.dp, brush = rimBrush, shape = shape)
    } else {
        Modifier
            .clip(shape)
            .background(resolvedTint)
            .border(width = 0.8.dp, brush = rimBrush, shape = shape)
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(glassModifier)
            .clip(shape)
            .then(clickModifier)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalCanvasBackdrop.current,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    tint: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit
) {
    val colors = SakuTheme.colors
    val resolvedTint = when {
        tint != Color.Unspecified -> tint
        isPrimary -> colors.primary
        else -> Color.Unspecified
    }
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier,
        enabled = enabled,
        tint = resolvedTint,
        surfaceColor = if (resolvedTint == Color.Unspecified) colors.glassSurface else Color.Unspecified,
        content = content
    )
}

@Composable
fun LiquidGlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = LocalCanvasBackdrop.current
) {
    LiquidToggle(
        selected = { checked },
        onSelect = onCheckedChange,
        backdrop = backdrop,
        modifier = modifier
    )
}

data class SakuBottomTabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

@Composable
fun LiquidGlassBottomBar(
    pagerState: PagerState,
    tabs: List<SakuBottomTabItem>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onTabClick: ((Int) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    LiquidGlassBottomBar(
        selectedIndex = pagerState.currentPage,
        onSelectTab = { index ->
            if (onTabClick != null) {
                onTabClick(index)
            } else {
                scope.launch {
                    if (pagerState.currentPage != index) {
                        pagerState.animateScrollToPage(index)
                    }
                }
            }
        },
        tabs = tabs,
        backdrop = backdrop,
        modifier = modifier
    )
}

@Composable
fun LiquidGlassBottomBar(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    tabs: List<SakuBottomTabItem>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val colors = SakuTheme.colors
    val useRenderEffect = isRenderEffectSupported()

    LiquidBottomTabs(
        selectedTabIndex = { selectedIndex },
        onTabSelected = onSelectTab,
        backdrop = backdrop,
        tabsCount = tabs.size,
        modifier = modifier
    ) {
        tabs.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val itemColor = if (!useRenderEffect && isSelected) {
                colors.primary
            } else {
                colors.textSecondary
            }

            LiquidBottomTab(onClick = { onSelectTab(index) }) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                    contentDescription = item.label,
                    tint = itemColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = item.label,
                    color = itemColor,
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
