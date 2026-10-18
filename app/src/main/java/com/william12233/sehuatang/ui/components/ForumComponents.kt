package com.william12233.sehuatang.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.william12233.sehuatang.data.model.ForumAttachment
import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.data.model.ForumSection
import com.william12233.sehuatang.data.model.ForumThread

@Composable
fun NativeFailureCard(
    failure: ForumFailure,
    onRetry: () -> Unit,
    onVerification: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("目前無法載入論壇資料", fontWeight = FontWeight.SemiBold)
            }
            Text(
                failure.userMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRetry) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("重試")
                }
                if (failure is ForumFailure.Verification && onVerification != null) {
                    OutlinedButton(onClick = onVerification) { Text("開啟隔離驗證") }
                }
            }
        }
    }
}

@Composable
fun LoadingThreadSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonLine(widthFraction = 0.86f, height = 18.dp)
                    SkeletonLine(widthFraction = 0.58f, height = 12.dp)
                    SkeletonLine(widthFraction = 0.72f, height = 12.dp)
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
fun SectionCard(section: ForumSection, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!section.iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = section.iconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(section.name.take(1)) }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(section.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (section.description.isNotBlank()) {
                    Text(
                        section.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val counts = listOfNotNull(
                    section.threadCount?.let { "文章 $it" },
                    section.postCount?.let { "回覆 $it" },
                ).joinToString("  ·  ")
                if (counts.isNotBlank()) {
                    Text(counts, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ThreadCard(thread: ForumThread, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (!thread.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = thread.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (thread.isPinned) Icon(Icons.Outlined.PushPin, contentDescription = "置頂", tint = MaterialTheme.colorScheme.primary)
                    if (thread.isLocked) Icon(Icons.Outlined.Lock, contentDescription = "鎖定", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        thread.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (thread.category.isNotBlank()) {
                    AssistChip(onClick = onClick, label = { Text(thread.category, maxLines = 1) })
                }
                Text(
                    listOfNotNull(thread.author.takeIf { it.isNotBlank() }, thread.createdAt.takeIf { it.isNotBlank() })
                        .joinToString("  ·  ")
                        .ifBlank { "論壇文章" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Metric(Icons.Outlined.ChatBubbleOutline, thread.replies?.toString() ?: "—", "回覆")
                    Metric(Icons.Outlined.Visibility, thread.views?.toString() ?: "—", "瀏覽")
                }
            }
        }
    }
}

@Composable
private fun Metric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$value $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AttachmentCard(attachment: ForumAttachment, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(attachment.title, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        attachment.fileName,
                        attachment.size,
                        if (attachment.requiresVerification) "需要驗證" else null,
                    ).joinToString("  ·  ").ifBlank { "論壇附件" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Icon(Icons.Outlined.OpenInNew, contentDescription = "下載附件")
        }
    }
}

@Composable
fun NativeLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

fun ForumFailure.userMessage(): String = when (this) {
    is ForumFailure.Network -> "網路連線失敗：$message"
    is ForumFailure.Http -> "論壇回傳 HTTP $code：$message"
    is ForumFailure.Parse -> message
    is ForumFailure.Verification -> message
    ForumFailure.AuthenticationRequired -> "這項操作需要先登入論壇。"
    is ForumFailure.PermissionDenied -> message
}
