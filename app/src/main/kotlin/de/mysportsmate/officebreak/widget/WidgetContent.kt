package de.mysportsmate.officebreak.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.mysportsmate.officebreak.MainActivity
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.ui.theme.GreenPrimary
import de.mysportsmate.officebreak.ui.theme.GreenPrimaryDarkTheme

private enum class WidgetMode { Ultra, Compact, Expanded }

@Composable
fun WidgetContent(
    context: Context,
    todayBreaks: Int,
    currentStreak: Int,
    timerStatus: String,
    remainingSeconds: Long = 0L,
) {
    GlanceTheme {
        val size = LocalSize.current
        val mode = when {
            size.height >= 100.dp -> WidgetMode.Expanded
            size.height >= 50.dp -> WidgetMode.Compact
            else -> WidgetMode.Ultra
        }

        val outerPaddingH = when (mode) {
            WidgetMode.Expanded -> 16.dp
            WidgetMode.Compact -> 8.dp
            WidgetMode.Ultra -> 8.dp
        }
        val outerPaddingV = when (mode) {
            WidgetMode.Expanded -> 10.dp
            WidgetMode.Compact -> 4.dp
            WidgetMode.Ultra -> 1.dp
        }
        val rowSpacing = when (mode) {
            WidgetMode.Expanded -> 8.dp
            WidgetMode.Compact -> 1.dp
            WidgetMode.Ultra -> 0.dp
        }
        val timerRowSpacing = when (mode) {
            WidgetMode.Expanded -> 8.dp
            WidgetMode.Compact -> 6.dp
            WidgetMode.Ultra -> 0.dp
        }
        val titleSize = when (mode) {
            WidgetMode.Expanded -> 18.sp
            WidgetMode.Compact -> 15.sp
            WidgetMode.Ultra -> 13.sp
        }
        val bodySize = when (mode) {
            WidgetMode.Expanded -> 15.sp
            WidgetMode.Compact -> 14.sp
            WidgetMode.Ultra -> 12.sp
        }
        val timerSize = when (mode) {
            WidgetMode.Expanded -> 16.sp
            WidgetMode.Compact -> 15.sp
            WidgetMode.Ultra -> 11.sp
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.background)
                .cornerRadius(16.dp)
                .padding(horizontal = outerPaddingH, vertical = outerPaddingV)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mode != WidgetMode.Ultra) {
                Text(
                    text = context.getString(R.string.app_name),
                    style = TextStyle(
                        color = WidgetColors.primary,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )

                Spacer(modifier = GlanceModifier.height(rowSpacing))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (todayBreaks > 0) {
                            context.getString(R.string.widget_today_breaks, todayBreaks)
                        } else {
                            context.getString(R.string.widget_today_no_breaks)
                        },
                        style = TextStyle(
                            color = WidgetColors.onBackground,
                            fontSize = bodySize,
                        ),
                        maxLines = 1,
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Text(
                        text = if (currentStreak > 0) {
                            context.getString(R.string.widget_streak, currentStreak)
                        } else {
                            context.getString(R.string.widget_no_streak)
                        },
                        style = TextStyle(
                            color = WidgetColors.onBackgroundSecondary,
                            fontSize = bodySize,
                        ),
                        maxLines = 1,
                    )
                }

                Spacer(modifier = GlanceModifier.height(timerRowSpacing))
            }

            TimerRow(
                context = context,
                timerStatus = timerStatus,
                remainingSeconds = remainingSeconds,
                mode = mode,
                timerFontSize = timerSize,
            )
        }
    }
}

@Composable
private fun TimerRow(
    context: Context,
    timerStatus: String,
    remainingSeconds: Long,
    mode: WidgetMode,
    timerFontSize: TextUnit,
) {
    val timerText = when (timerStatus) {
        WidgetTimerState.STATUS_RUNNING -> {
            if (remainingSeconds > 0) {
                val formatted = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
                context.getString(R.string.widget_timer_remaining, formatted)
            } else {
                context.getString(R.string.widget_timer_running)
            }
        }
        WidgetTimerState.STATUS_PAUSED -> {
            if (remainingSeconds > 0) {
                val formatted = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
                context.getString(R.string.widget_timer_paused, formatted)
            } else {
                context.getString(R.string.widget_timer_running)
            }
        }
        WidgetTimerState.STATUS_EXPIRED -> context.getString(R.string.widget_timer_expired)
        else -> context.getString(R.string.widget_timer_idle)
    }

    val timerColor = when (timerStatus) {
        WidgetTimerState.STATUS_EXPIRED -> WidgetColors.primary
        WidgetTimerState.STATUS_RUNNING, WidgetTimerState.STATUS_PAUSED -> WidgetColors.onBackground
        else -> WidgetColors.onBackgroundSecondary
    }

    val isIdle = timerStatus == WidgetTimerState.STATUS_IDLE
    val buttonLabel = if (isIdle) {
        if (mode == WidgetMode.Expanded) context.getString(R.string.widget_start) else "▶"
    } else {
        if (mode == WidgetMode.Expanded) context.getString(R.string.widget_open) else "↗"
    }
    val buttonAction: Action = if (isIdle) {
        actionRunCallback<StartTimerAction>()
    } else {
        actionStartActivity<MainActivity>()
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = GlanceModifier.defaultWeight(),
            text = timerText,
            style = TextStyle(
                color = timerColor,
                fontSize = timerFontSize,
                fontWeight = if (timerStatus == WidgetTimerState.STATUS_EXPIRED) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = 1,
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        PillButton(
            label = buttonLabel,
            onClick = buttonAction,
            mode = mode,
        )
    }
}

@Composable
private fun PillButton(
    label: String,
    onClick: Action,
    mode: WidgetMode,
) {
    val hPad = when (mode) {
        WidgetMode.Expanded -> 16.dp
        WidgetMode.Compact -> 10.dp
        WidgetMode.Ultra -> 8.dp
    }
    val vPad = when (mode) {
        WidgetMode.Expanded -> 10.dp
        WidgetMode.Compact -> 4.dp
        WidgetMode.Ultra -> 2.dp
    }
    val fontSize = when (mode) {
        WidgetMode.Expanded -> 14.sp
        WidgetMode.Compact -> 14.sp
        WidgetMode.Ultra -> 11.sp
    }

    Box(
        modifier = GlanceModifier
            .background(WidgetColors.buttonBackground)
            .cornerRadius(20.dp)
            .padding(horizontal = hPad, vertical = vPad)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = WidgetColors.buttonContent,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

private object WidgetColors {
    val background = ColorProvider(
        day = androidx.compose.ui.graphics.Color.White,
        night = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    )
    val primary = ColorProvider(
        day = GreenPrimary,
        night = GreenPrimaryDarkTheme,
    )
    val onBackground = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
        night = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    )
    val onBackgroundSecondary = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFF49454F),
        night = androidx.compose.ui.graphics.Color(0xFFCAC4D0),
    )
    val buttonBackground = ColorProvider(
        day = GreenPrimary,
        night = GreenPrimaryDarkTheme,
    )
    val buttonContent = ColorProvider(
        day = androidx.compose.ui.graphics.Color.White,
        night = de.mysportsmate.officebreak.ui.theme.OnGreenPrimaryDark,
    )
}
