package com.kkokkkok.android.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kkokkkok.android.R

// Read-only source: Figma home 26:141, shared color and text styles.
object KkokColors {
    val Background = Color(0xFFFAF7F5)
    val White = Color.White
    val Primary = Color(0xFFFF9A3D)
    val Secondary = Color(0xFFFFEBD6)
    val Point = Color(0xFFFFD45C)
    val Ink = Color(0xFF3D2C22)
    val Gray600 = Color(0xFF756A65)
    val Gray500 = Color(0xFF938781)
    val Gray400 = Color(0xFFB3A7A1)
    val Gray200 = Color(0xFFE5DDD8)
    val Gray100 = Color(0xFFF2EDEA)
    val High = Color(0xFFFF6F61)
    val Safe = Color(0xFF5CC08A)
    val Kakao = Color(0xFFFEE500)
    val Naver = Color(0xFF03C75A)
}

object KkokTypography {
    private val jua = FontFamily(Font(R.font.jua_regular))
    private val gowunDodum = FontFamily(Font(R.font.gowun_dodum_regular))

    private fun style(font: FontFamily, size: Int, height: Int, spacing: Float = 0f) = TextStyle(
        fontFamily = font,
        fontSize = size.sp,
        lineHeight = height.sp,
        letterSpacing = spacing.sp,
        color = KkokColors.Ink,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        lineBreak = LineBreak.Heading,
    )

    val HeadlineMedium = style(jua, 28, 36)
    val HeadlineSmall = style(jua, 24, 32)
    val TitleLarge = style(jua, 22, 28)
    val TitleMedium = style(jua, 16, 24, .15f)
    val BodyMedium = style(gowunDodum, 14, 20, .25f)
    val BodySmall = style(gowunDodum, 12, 16, .4f)
    val LabelMedium = style(gowunDodum, 12, 16, .5f)
    val LabelLarge = style(gowunDodum, 14, 20, .1f)
    val LabelSmall = style(gowunDodum, 11, 16, .5f)
    val Score = style(jua, 48, 52)
}

object KkokSpacing {
    val Page = 20.dp
    val Section = 20.dp
    val Card = 16.dp
    val Item = 12.dp
    val Small = 8.dp
}

@Composable
fun KkokText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = KkokTypography.BodyMedium,
    color: Color = style.color,
) {
    BasicText(text, modifier, style.copy(color = color))
}

@Composable
fun FigmaIcon(@DrawableRes resource: Int, modifier: Modifier = Modifier) {
    // Keep source colors and natural vector dimensions; no Material icon substitution or tint.
    Image(painterResource(resource), contentDescription = null, modifier = modifier)
}

@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    diameter: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(diameter).background(color, CircleShape), contentAlignment = Alignment.Center) {
        FigmaIcon(icon)
    }
}
