package zm.co.codelabs.progress

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

private data class YearStats(
    val year: Int,
    val pct01: Double,
    val pctText: String,
    val dayOfYear: Int,
    val daysInYear: Int,
    val daysLeft: Int,
)

private fun yearStats(now: ZonedDateTime = ZonedDateTime.now()): YearStats {
    val year = now.year
    val start = now.withDayOfYear(1).toLocalDate().atStartOfDay(now.zone)
    val end = start.plusYears(1)

    val totalMs = end.toInstant().toEpochMilli() - start.toInstant().toEpochMilli()
    val doneMs = now.toInstant().toEpochMilli() - start.toInstant().toEpochMilli()

    val pct01 = (doneMs.toDouble() / totalMs.toDouble()).coerceIn(0.0, 1.0)
    val pctText = String.format(Locale.US, "%.2f", pct01 * 100.0)

    val dayOfYear = now.dayOfYear
    val daysInYear = now.toLocalDate().lengthOfYear()
    val daysLeft = (daysInYear - dayOfYear).coerceAtLeast(0)

    return YearStats(year, pct01, pctText, dayOfYear, daysInYear, daysLeft)
}

class YearProgressGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 90.dp),
            DpSize(250.dp, 140.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val stats = yearStats(ZonedDateTime.now(ZoneId.systemDefault()))
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .background(GlanceTheme.colors.surface)
                        .clickable(actionRunCallback<RefreshCallback>()),
                ) {
                    Header(stats)

                    Spacer(GlanceModifier.height(14.dp))

                    ProgressPill(pct = stats.pct01.toFloat())

                    Spacer(GlanceModifier.height(12.dp))

                    Footer(stats)
                }
            }
        }
    }

    @Composable
    private fun Header(stats: YearStats) {
        val c = GlanceTheme.colors

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "${stats.pctText}%",
                    style = TextStyle(fontWeight = FontWeight.Bold, color = c.onSurface)
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = "of the year has passed",
                    style = TextStyle(color = c.onSurfaceVariant)
                )
            }

            Text(
                text = "${stats.year}",
                modifier = GlanceModifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    .background(c.primaryContainer),
                style = TextStyle(fontWeight = FontWeight.Medium, color = c.onPrimaryContainer)
            )
        }
    }

    @Composable
    private fun ProgressPill(pct: Float) {
        val c = GlanceTheme.colors

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(24.dp)
                .background(c.onSurfaceVariant)
                .cornerRadius(12.dp)
        ) {
            val progressWidth = (pct.coerceIn(0f, 1f) * 200).dp
            Box(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .width(progressWidth)
                    .background(c.primary)
                    .cornerRadius(12.dp)
            ) {}
        }
    }

    @Composable
    private fun Footer(stats: YearStats) {
        val c = GlanceTheme.colors

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Stat("Day", "${stats.dayOfYear}/${stats.daysInYear}")
            Spacer(GlanceModifier.width(16.dp))
            Stat("Left", "${stats.daysLeft}d")
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "Tap to refresh",
                style = TextStyle(color = c.onSurfaceVariant)
            )
        }
    }

    fun openCalendarAction(context: Context): Action {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return actionStartActivity(intent)
    }

    @Composable
    private fun Stat(label: String, value: String) {
        val c = GlanceTheme.colors
        Column {
            Text(
                text = label,
                style = TextStyle(color = c.onSurfaceVariant),
            )
            if (label == "Day") {
                Text(
                    text = value,
                    style = TextStyle(fontWeight = FontWeight.Medium, color = c.onSurface),
                    modifier = GlanceModifier
                        .clickable(openCalendarAction(context = LocalContext.current))
                )
            } else {
                Text(
                    text = value,
                    style = TextStyle(fontWeight = FontWeight.Medium, color = c.onSurface)
                )
            }
        }
    }
}

class RefreshCallback : androidx.glance.appwidget.action.ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        YearProgressGlanceWidget().update(context, glanceId)
    }
}

class YearProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = YearProgressGlanceWidget()
}
