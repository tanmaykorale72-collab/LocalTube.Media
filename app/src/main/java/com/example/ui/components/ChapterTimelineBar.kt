package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ChapterEntity
import com.example.player.MediaPlaybackController

/**
 * YouTube-inspired playback timeline with chapter segments and live scrub preview.
 * Does not fabricate chapters if [chapters] is empty.
 */
@Composable
fun ChapterTimelineBar(
    currentPositionMs: Long,
    durationMs: Long,
    chapters: List<ChapterEntity>,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    playedColor: Color = MaterialTheme.colorScheme.primary,
    unplayedColor: Color = Color.White.copy(alpha = 0.28f),
    thumbColor: Color = MaterialTheme.colorScheme.secondary
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (isDragging) {
        dragFraction.coerceIn(0f, 1f)
    } else {
        (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }

    val previewTimeMs = (displayFraction * safeDuration).toLong()
    val previewChapter = remember(chapters, previewTimeMs) {
        MediaPlaybackController.findActiveChapter(chapters, previewTimeMs)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating preview pill while scrubbing
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .background(
                        color = Color(0xE6111520),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                val previewLabel = if (previewChapter != null) {
                    "${FormatUtils.formatDuration(previewTimeMs)}  •  ${previewChapter.title}"
                } else {
                    FormatUtils.formatDuration(previewTimeMs)
                }
                Text(
                    text = previewLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag("player_timeline_scrubber")
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeekTo((fraction * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val targetMs = (dragFraction * safeDuration).toLong()
                            isDragging = false
                            onSeekTo(targetMs)
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                val widthPx = size.width
                val centerY = size.height / 2f
                val baseTrackHeight = if (isDragging) 6.dp.toPx() else 4.dp.toPx()
                val activeSegmentHeight = if (isDragging) 9.dp.toPx() else 6.dp.toPx()
                val thumbX = (displayFraction * widthPx).coerceIn(0f, widthPx)

                if (chapters.size >= 2 && durationMs > 0L) {
                    // Draw segmented YouTube-style chapter bars with a 3dp gap between chapters
                    val gapPx = 3.dp.toPx()
                    val sortedStarts = chapters.map { it.startTimeMs.coerceIn(0L, safeDuration) }
                        .distinct()
                        .sorted()
                    val boundaries = buildList {
                        if (sortedStarts.first() > 0L) add(0L)
                        addAll(sortedStarts)
                        if (last() < safeDuration) add(safeDuration)
                    }

                    for (i in 0 until boundaries.lastIndex) {
                        val segStartMs = boundaries[i]
                        val segEndMs = boundaries[i + 1]
                        val startX = (segStartMs.toFloat() / safeDuration) * widthPx +
                            if (i == 0) 0f else gapPx / 2f
                        val endX = (segEndMs.toFloat() / safeDuration) * widthPx -
                            if (i == boundaries.lastIndex - 1) 0f else gapPx / 2f

                        if (endX <= startX) continue
                        val segWidth = endX - startX
                        val isCurrentSeg = thumbX in startX..endX
                        val segH = if (isCurrentSeg) activeSegmentHeight else baseTrackHeight
                        val topY = centerY - segH / 2f

                        // Background unplayed segment
                        drawRoundRect(
                            color = unplayedColor,
                            topLeft = Offset(startX, topY),
                            size = Size(segWidth, segH),
                            cornerRadius = CornerRadius(segH / 2f, segH / 2f)
                        )

                        // Foreground played portion of segment
                        if (thumbX > startX) {
                            val playedW = (thumbX - startX).coerceAtMost(segWidth)
                            drawRoundRect(
                                color = playedColor,
                                topLeft = Offset(startX, topY),
                                size = Size(playedW, segH),
                                cornerRadius = CornerRadius(segH / 2f, segH / 2f)
                            )
                        }
                    }
                } else {
                    // Standard continuous timeline when no chapters exist
                    val topY = centerY - baseTrackHeight / 2f
                    drawRoundRect(
                        color = unplayedColor,
                        topLeft = Offset(0f, topY),
                        size = Size(widthPx, baseTrackHeight),
                        cornerRadius = CornerRadius(baseTrackHeight / 2f, baseTrackHeight / 2f)
                    )
                    if (thumbX > 0f) {
                        drawRoundRect(
                            color = playedColor,
                            topLeft = Offset(0f, topY),
                            size = Size(thumbX, baseTrackHeight),
                            cornerRadius = CornerRadius(baseTrackHeight / 2f, baseTrackHeight / 2f)
                        )
                    }
                }

                // Scrubber thumb handle
                val thumbRadius = if (isDragging) 8.5.dp.toPx() else 6.5.dp.toPx()
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(thumbX, centerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius * 0.45f,
                    center = Offset(thumbX, centerY)
                )
            }
        }
    }
}
