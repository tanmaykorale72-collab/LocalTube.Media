package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChapterEntity
import com.example.data.scanner.MediaScannerService
import com.example.player.MediaPlaybackController
import com.example.ui.components.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `mainActivity launches and renders bottom navigation and home screen`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LocalTube", appName)

        composeTestRule.onNodeWithTag("main_bottom_navigation").assertExists()
        composeTestRule.onNodeWithTag("home_screen_root").assertExists()
    }

    @Test
    fun `chapter parser extracts valid timestamps and ignores non-chapter lines`() {
        val rawSidecar = """
            00:00 Opening & Visual Framing
            01:25 Color Grading Atmosphere
            1:02:10 Final Director Cut
            Random note without timestamp
        """.trimIndent()

        val chapters = MediaScannerService.parseTimestampLines(
            mediaUri = "content://media/video/1",
            rawText = rawSidecar,
            durationMs = 4_000_000L,
            isAuto = true
        )

        assertEquals(3, chapters.size)
        assertEquals(0L, chapters[0].startTimeMs)
        assertEquals("Opening & Visual Framing", chapters[0].title)
        assertEquals(85_000L, chapters[1].startTimeMs)
        assertEquals("Color Grading Atmosphere", chapters[1].title)
        assertEquals(3_730_000L, chapters[2].startTimeMs)
        assertEquals("Final Director Cut", chapters[2].title)
    }

    @Test
    fun `active chapter lookup finds matching chapter and returns null when empty`() {
        val emptyResult = MediaPlaybackController.findActiveChapter(emptyList(), 15_000L)
        assertNull(emptyResult)

        val chapters = listOf(
            ChapterEntity(id = 1, mediaUri = "u", startTimeMs = 0L, title = "Intro"),
            ChapterEntity(id = 2, mediaUri = "u", startTimeMs = 10_000L, title = "Part 1"),
            ChapterEntity(id = 3, mediaUri = "u", startTimeMs = 30_000L, title = "Part 2")
        )
        assertEquals("Intro", MediaPlaybackController.findActiveChapter(chapters, 5_000L)?.title)
        assertEquals("Part 1", MediaPlaybackController.findActiveChapter(chapters, 15_000L)?.title)
        assertEquals("Part 2", MediaPlaybackController.findActiveChapter(chapters, 45_000L)?.title)
    }

    @Test
    fun `shorts candidate logic identifies vertical and short duration videos`() {
        assertTrue(
            MediaScannerService.isShortVideoCandidate(
                durationMs = 45_000L,
                width = 1080,
                height = 1920,
                maxShortSec = 90,
                includeHorizontalShorts = false
            )
        )
        assertEquals("12:05", FormatUtils.formatDuration(725_000L))
    }
}
