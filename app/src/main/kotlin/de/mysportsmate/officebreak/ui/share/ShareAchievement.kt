package de.mysportsmate.officebreak.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.VisibleForTesting
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import de.mysportsmate.officebreak.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

private const val CARD_WIDTH = 1080
private const val CARD_HEIGHT = 1350

private const val COLOR_GREEN_PRIMARY = 0xFF009900.toInt()
private const val COLOR_GREEN_DARK = 0xFF006D00.toInt()

private const val CONFETTI_COUNT = 60

private val CONFETTI_COLORS = intArrayOf(
    0xFFFF6B6B.toInt(),
    0xFF4ECDC4.toInt(),
    0xFFFFE66D.toInt(),
    0xFF95E1D3.toInt(),
    0xFFF38181.toInt(),
    0xFF6C5CE7.toInt(),
)

suspend fun shareAchievement(
    context: Context,
    title: String,
    description: String,
) {
    val file = withContext(Dispatchers.Default) {
        val bitmap = renderShareCard(context, title, description)
        saveBitmapToCache(context, bitmap)
    }
    launchShareIntent(context, file, title)
}

private fun renderShareCard(
    context: Context,
    title: String,
    description: String,
): Bitmap {
    val unlockLabel = context.getString(R.string.share_card_unlocked)
    val appName = context.getString(R.string.share_card_app_name)

    val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background gradient
    val gradientPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, CARD_HEIGHT.toFloat(),
            COLOR_GREEN_DARK, COLOR_GREEN_PRIMARY,
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), gradientPaint)

    // Confetti
    drawConfetti(canvas)

    // Trophy emoji
    val trophyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 160f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("\uD83C\uDFC6", CARD_WIDTH / 2f, CARD_HEIGHT * 0.32f, trophyPaint)

    // "Achievement Unlocked!" label
    val unlockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xD9FFFFFF.toInt()
        textSize = 44f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        letterSpacing = 0.05f
    }
    canvas.drawText(unlockLabel, CARD_WIDTH / 2f, CARD_HEIGHT * 0.42f, unlockPaint)

    // Achievement title
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 72f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    drawWrappedText(canvas, title, titlePaint, CARD_WIDTH / 2f, CARD_HEIGHT * 0.50f, CARD_WIDTH - 160f)

    // Achievement description
    val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCCFFFFFF.toInt()
        textSize = 44f
        textAlign = Paint.Align.CENTER
    }
    val titleLines = countWrappedLines(title, titlePaint, CARD_WIDTH - 160f)
    val descY = CARD_HEIGHT * 0.50f + titleLines * titlePaint.textSize * 1.3f + 40f
    drawWrappedText(canvas, description, descPaint, CARD_WIDTH / 2f, descY, CARD_WIDTH - 160f)

    // App icon + "Office Break" watermark
    drawWatermark(context, canvas, appName)

    return bitmap
}

private fun drawConfetti(canvas: Canvas) {
    val random = Random(42)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    for (i in 0 until CONFETTI_COUNT) {
        val x = random.nextFloat() * CARD_WIDTH
        val y = random.nextFloat() * CARD_HEIGHT
        val radius = 6f + random.nextFloat() * 14f
        val alpha = (0.3f + random.nextFloat() * 0.5f)
        val color = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.size)]

        paint.color = color
        paint.alpha = (alpha * 255).toInt()

        if (random.nextBoolean()) {
            canvas.drawCircle(x, y, radius, paint)
        } else {
            canvas.drawRect(x - radius, y - radius * 0.6f, x + radius, y + radius * 0.6f, paint)
        }
    }
}

private fun drawWatermark(context: Context, canvas: Canvas, appName: String) {
    val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80FFFFFF.toInt()
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        letterSpacing = 0.1f
    }

    val textBounds = Rect()
    watermarkPaint.getTextBounds(appName, 0, appName.length, textBounds)
    val textWidth = textBounds.width()

    val iconSize = 72
    val gap = 16
    val totalWidth = iconSize + gap + textWidth
    val startX = (CARD_WIDTH - totalWidth) / 2f
    val centerY = CARD_HEIGHT - 80f

    val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.ic_launcher_foreground, null)
    if (drawable != null) {
        val iconLeft = startX.toInt()
        val iconTop = (centerY - iconSize / 2f - textBounds.height() / 2f).toInt()
        drawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        drawable.alpha = 128
        drawable.draw(canvas)
    }

    watermarkPaint.textAlign = Paint.Align.LEFT
    canvas.drawText(appName, startX + iconSize + gap, centerY, watermarkPaint)
}

private fun drawWrappedText(canvas: Canvas, text: String, paint: Paint, x: Float, y: Float, maxWidth: Float) {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = ""
    val bounds = Rect()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        paint.getTextBounds(testLine, 0, testLine.length, bounds)
        if (bounds.width() > maxWidth && currentLine.isNotEmpty()) {
            lines.add(currentLine)
            currentLine = word
        } else {
            currentLine = testLine
        }
    }
    if (currentLine.isNotEmpty()) lines.add(currentLine)

    val lineHeight = paint.textSize * 1.3f
    for ((i, line) in lines.withIndex()) {
        canvas.drawText(line, x, y + i * lineHeight, paint)
    }
}

private fun countWrappedLines(text: String, paint: Paint, maxWidth: Float): Int {
    val words = text.split(" ")
    var lines = 1
    var currentLine = ""
    val bounds = Rect()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        paint.getTextBounds(testLine, 0, testLine.length, bounds)
        if (bounds.width() > maxWidth && currentLine.isNotEmpty()) {
            lines++
            currentLine = word
        } else {
            currentLine = testLine
        }
    }

    return lines
}

@VisibleForTesting
internal fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
    val dir = File(context.cacheDir, "share_images")
    dir.mkdirs()
    val file = File(dir, "achievement_share.png")
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    return file
}

@VisibleForTesting
internal fun buildShareIntent(context: Context, file: File, title: String): Intent {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    return Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text, title))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun launchShareIntent(context: Context, file: File, title: String) {
    val shareIntent = buildShareIntent(context, file, title)
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_title)),
    )
}
