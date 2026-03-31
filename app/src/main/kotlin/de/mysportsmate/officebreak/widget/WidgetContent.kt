package de.mysportsmate.officebreak.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import de.mysportsmate.officebreak.MainActivity
import de.mysportsmate.officebreak.R
import de.mysportsmate.officebreak.ui.theme.GreenPrimary
import de.mysportsmate.officebreak.ui.theme.GreenPrimaryDarkTheme

@Composable
fun WidgetContent(
    context: Context,
    todayBreaks: Int,
    currentStreak: Int,
    timerStatus: String,
) {
    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetColors.background)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            // Title
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(
                    color = WidgetColors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Stats row
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
                        fontSize = 13.sp,
                    ),
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
                        fontSize = 13.sp,
                    ),
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Timer row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (timerStatus) {
                        "running" -> context.getString(R.string.widget_timer_running)
                        "expired" -> context.getString(R.string.widget_timer_expired)
                        else -> context.getString(R.string.widget_timer_idle)
                    },
                    style = TextStyle(
                        color = if (timerStatus == "expired") {
                            WidgetColors.primary
                        } else {
                            WidgetColors.onBackgroundSecondary
                        },
                        fontSize = 13.sp,
                        fontWeight = if (timerStatus == "expired") FontWeight.Bold else FontWeight.Normal,
                    ),
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                if (timerStatus == "idle") {
                    Button(
                        text = context.getString(R.string.widget_start),
                        onClick = actionRunCallback<StartTimerAction>(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = WidgetColors.buttonBackground,
                            contentColor = WidgetColors.buttonContent,
                        ),
                    )
                } else {
                    Button(
                        text = context.getString(R.string.widget_open),
                        onClick = actionStartActivity<MainActivity>(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = WidgetColors.buttonBackground,
                            contentColor = WidgetColors.buttonContent,
                        ),
                    )
                }
            }
        }
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
        night = androidx.compose.ui.graphics.Color(0xFF003A00),
    )
}
