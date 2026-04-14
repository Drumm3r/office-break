package de.mysportsmate.officebreak.ui.share

import android.content.Context
import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.OutputStream

class ShareAchievementTest {

    private lateinit var context: Context
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        cacheDir = File(System.getProperty("java.io.tmpdir"), "test_cache_${System.nanoTime()}")
        cacheDir.mkdirs()

        context = mockk(relaxed = true)
        every { context.cacheDir } returns cacheDir
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun `saveBitmapToCache creates file in share_images directory`() {
        val bitmap = mockBitmap()

        val file = saveBitmapToCache(context, bitmap)

        assertEquals("achievement_share.png", file.name)
        assertEquals("share_images", file.parentFile?.name)
        assertTrue(file.exists())
    }

    @Test
    fun `saveBitmapToCache creates parent directories`() {
        val freshCacheDir = File(System.getProperty("java.io.tmpdir"), "fresh_${System.nanoTime()}")
        val freshContext = mockk<Context>(relaxed = true)
        every { freshContext.cacheDir } returns freshCacheDir

        val bitmap = mockBitmap()
        val file = saveBitmapToCache(freshContext, bitmap)

        assertTrue(file.parentFile!!.exists())
        assertTrue(file.exists())

        freshCacheDir.deleteRecursively()
    }

    @Test
    fun `saveBitmapToCache calls compress with PNG format`() {
        val bitmap = mockBitmap()

        saveBitmapToCache(context, bitmap)

        verify {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, any<OutputStream>())
        }
    }

    @Test
    fun `saveBitmapToCache returns file under context cacheDir`() {
        val bitmap = mockBitmap()

        val file = saveBitmapToCache(context, bitmap)

        assertTrue(file.absolutePath.startsWith(cacheDir.absolutePath))
    }

    @Test
    fun `saveBitmapToCache overwrites on repeated calls`() {
        val bitmap = mockBitmap()

        val file1 = saveBitmapToCache(context, bitmap)
        val file2 = saveBitmapToCache(context, bitmap)

        assertEquals(file1.absolutePath, file2.absolutePath)
    }

    private fun mockBitmap(): Bitmap {
        val bitmap = mockk<Bitmap>()
        every {
            bitmap.compress(any(), any(), any<OutputStream>())
        } answers {
            val stream = thirdArg<OutputStream>()
            stream.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
            true
        }

        return bitmap
    }
}
