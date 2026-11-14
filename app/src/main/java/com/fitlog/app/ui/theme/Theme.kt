package com.fitlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import io.github.neoruaa.glasmio.core.GlasmioShapes
import io.github.neoruaa.glasmio.core.GlasmioSpacing
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.GlassAppearance
import io.github.neoruaa.glasmio.core.GlassDimensions
import io.github.neoruaa.glasmio.core.defaultGlassColors
import io.github.neoruaa.glasmio.core.glasmioColorScheme
import io.github.neoruaa.glasmio.core.glasmioTypography

/**
 * 应用自己的强调色与营养配色。
 *
 * 其余语义色（背景、分组底色、分隔线、次要文字、危险色）全部使用 Glasmio 的
 * iOS 调色板，跟随系统深浅色；这里只保留「FitLog 品牌色」和三项宏量营养色，
 * 这样整套界面的明暗关系由框架统一保证。
 */
object FitPalette {

    /** 品牌绿：取 iOS 系统绿，深浅色各一档。 */
    fun accent(dark: Boolean): Color = if (dark) Color(0xFF30D158) else Color(0xFF34C759)

    /** 强调色上面的文字颜色：白字在浅色绿上对比度更高，深色下用黑字更清楚。 */
    fun onAccent(dark: Boolean): Color = if (dark) Color(0xFF06280F) else Color.White

    /** 蛋白 / 碳水 / 脂肪，取自 iOS 系统红、蓝、橙。 */
    val protein = Color(0xFFFF375F)
    val carbs = Color(0xFF0A84FF)
    val fat = Color(0xFFFF9F0A)

    /** 超出目标时的提示色。 */
    fun danger(dark: Boolean): Color = if (dark) Color(0xFFFF453A) else Color(0xFFFF3B30)
}

/**
 * 全局主题：把 Glasmio 的 iOS 主题套上 FitLog 的强调色。
 *
 * 这里所有主题对象都用 `remember(dark)` 缓存。GlasmioTheme 的默认参数是每次调用
 * 现算的（配色、字体、圆角、间距……），如果每次都传新实例，`LocalGlassColors`
 * 之类的 CompositionLocal 就会被判定成「变了」，导致整棵树连同玻璃材质一起重组——
 * 打字、弹提示、切日期时都会卡一下。缓存之后只有切换深浅色才会重建。
 */
@Composable
fun FitLogTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val accent = FitPalette.accent(dark)
    val colorScheme = remember(dark, accent) { glasmioColorScheme(dark, accent) }
    val colors = remember(dark, accent) { defaultGlassColors(dark, accent) }
    val typography = remember { glasmioTypography() }
    val appearance = remember { GlassAppearance() }
    val dimensions = remember { GlassDimensions() }
    val shapes = remember { GlasmioShapes() }
    val spacing = remember { GlasmioSpacing() }

    GlasmioTheme(
        isDark = dark,
        colorScheme = colorScheme,
        typography = typography,
        colors = colors,
        appearance = appearance,
        dimensions = dimensions,
        shapes = shapes,
        spacing = spacing,
        content = content,
    )
}
