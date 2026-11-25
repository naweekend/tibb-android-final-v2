package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterEyebrowStyle
import com.example.ui.theme.InterHeroStatStyle
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TabularNumberStyle
import com.example.ui.theme.urduTextStyle

/**
 * Clean section header enforcing clear visual hierarchy (eyebrow -> title -> optional subtitle -> trailing).
 */
@Composable
fun TibbSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    isUr: Boolean = false,
    accentColor: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = LocalTibbColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (!eyebrow.isNullOrBlank()) {
                Text(
                    text = eyebrow.uppercase(),
                    style = InterEyebrowStyle,
                    color = accentColor ?: c.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.titleLarge) else MaterialTheme.typography.titleLarge,
                color = c.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                    color = c.muted,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Soft semantic status badge combining gentle tinted container, hairline border, icon, and text label.
 */
@Composable
fun TibbStatusBadge(
    text: String,
    color: Color,
    softBg: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(softBg)
            .border(0.75.dp, color.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Compact tabular stat block with Inter Bold value, unit, and subordinate caption.
 */
@Composable
fun TibbStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    accentColor: Color? = null,
    containerColor: Color? = null,
) {
    val c = LocalTibbColors.current
    val resolvedAccent = accentColor ?: c.accent
    val resolvedBg = containerColor ?: c.surfaceMuted.copy(alpha = 0.72f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(resolvedBg)
            .border(0.75.dp, resolvedAccent.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = c.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = value,
                style = InterHeroStatStyle.copy(fontSize = 17.sp, lineHeight = 20.sp),
                color = resolvedAccent,
                maxLines = 1,
            )
            if (!unit.isNullOrBlank()) {
                Text(
                    text = unit,
                    style = TabularNumberStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = c.muted,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Scannable clinical instruction block with distinct semantic tint, clear icon header,
 * and numbered circular badges for step-by-step First Aid and Safe Home Care.
 */
@Composable
fun TibbMedicalInfoBlock(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    items: List<String>,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    numbered: Boolean = true,
    isUr: Boolean = false,
) {
    val c = LocalTibbColors.current
    if (items.isEmpty()) return
    val bg = containerColor ?: c.surfaceMuted.copy(alpha = if (c.isDark) 0.58f else 0.7f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(0.75.dp, accentColor.copy(alpha = 0.26f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (c.isDark) 0.22f else 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
            }
            Text(
                text = title,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.labelLarge) else MaterialTheme.typography.labelLarge,
                color = accentColor,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { idx, line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (numbered) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = if (c.isDark) 0.24f else 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = TabularNumberStyle.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                                color = accentColor,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                        )
                    }
                    Text(
                        text = line,
                        style = if (isUr) urduTextStyle(MaterialTheme.typography.bodySmall) else MaterialTheme.typography.bodySmall,
                        color = c.ink,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Calm clinical callout card for precautions, doctor consultation rules, and safety alerts.
 */
@Composable
fun TibbClinicalCallout(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    isUr: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(0.75.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
            Text(
                text = title,
                style = if (isUr) urduTextStyle(MaterialTheme.typography.labelLarge) else MaterialTheme.typography.labelLarge,
                color = accentColor,
            )
        }
        content()
    }
}
