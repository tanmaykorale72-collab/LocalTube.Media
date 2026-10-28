package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ChapterEntity
import com.example.data.local.MediaItemEntity
import com.example.ui.theme.CardToneForestEnd
import com.example.ui.theme.CardToneForestStart
import com.example.ui.theme.CardToneSlateEnd
import com.example.ui.theme.CardToneSlateStart
import com.example.ui.theme.CardToneUmberEnd
import com.example.ui.theme.CardToneUmberStart
import com.example.ui.theme.ProgressRed
import java.io.File
import kotlin.math.abs
import kotlin.math.sin

/**
 * Media card matching the supplied reference screenshot:
 * - Rounded 16:9 thumbnail with Forest Teal / Warm Umber tonal palette & centered play glyph
 * - Bottom-right dark duration pill ("12:04")
 * - Bottom red progress bar (#E53935) when partially watched
 * - Below thumbnail: dark circular avatar, 2-line white title, "Folder · 12:04 · added 2d ago" subtitle, and 3-dot icon
 */
@Composable
fun MediaCard(
    item: MediaItemEntity,
    chapters: List<ChapterEntity>,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = if (item.durationMs > 0L && item.lastPlayedPositionMs > 2_000L) {
        (item.lastPlayedPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("media_card_${item.title.take(20)}")
            .clickable(onClick = onClick)
    ) {
        // 16:9 Media Thumbnail Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardToneForestEnd)
        ) {
            val hasCachedThumb = item.thumbnailPath != null && File(item.thumbnailPath).exists()

            if (hasCachedThumb) {
                AsyncImage(
                    model = File(item.thumbnailPath!!),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Subtle dark tint & center play triangle matching the reference photo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(34.dp)
                    )
                }
            } else {
                MediaArtworkCanvas(
                    title = item.title,
                    isAudio = item.isAudio,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Optional discreet top-left badge for Audio or Chapters
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.isAudio) {
                    Surface(
                        color = Color(0xCC0D0D0D),
                        contentColor = Color(0xFFD6ECE6),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (chapters.isNotEmpty()) {
                    Surface(
                        color = Color(0xCC0D0D0D),
                        contentColor = Color(0xFFD6ECE6),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${chapters.size} Chapters",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Bottom-right duration badge matching the screenshot
            if (item.durationMs > 0L) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = if (progressFraction > 0f) 10.dp else 8.dp),
                    color = Color(0xD90A0A0A),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = FormatUtils.formatDuration(item.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Bottom coral-red watched progress bar matching the screenshot
            if (progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = ProgressRed,
                    trackColor = Color.White.copy(alpha = 0.16f)
                )
            }
        }

        // Metadata Row below thumbnail matching the screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 2.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Subtle dark circle avatar on the left (matching the screenshot)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                val metaLine = buildList {
                    add(item.folderName)
                    if (item.durationMs > 0L) {
                        add(FormatUtils.formatDuration(item.durationMs))
                    } else {
                        add(item.resolutionLabel)
                    }
                    add(FormatUtils.formatRelativeTime(item.lastModifiedMs))
                }.joinToString(" · ")

                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Media details and chapters",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MediaArtworkCanvas(
    title: String,
    isAudio: Boolean,
    modifier: Modifier = Modifier
) {
    val seed = rememberSeed(title)
    val paletteIndex = seed % 3
    val (startColor, endColor) = when (paletteIndex) {
        0 -> CardToneForestStart to CardToneForestEnd
        1 -> CardToneUmberStart to CardToneUmberEnd
        else -> CardToneSlateStart to CardToneSlateEnd
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(startColor, endColor)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        if (isAudio) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = 24
                val spacing = size.width / (barCount + 6)
                val barWidth = spacing * 0.5f
                val centerY = size.height / 2f
                for (i in 0 until barCount) {
                    val wave = abs(sin((i + seed) * 0.45f))
                    val barH = (size.height * 0.10f) + (size.height * 0.36f * wave)
                    val x = spacing * (i + 3)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.12f + 0.15f * wave),
                        topLeft = Offset(x, centerY - barH / 2f),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun rememberSeed(text: String): Int = text.hashCode() and 0x7FFFFFFF
