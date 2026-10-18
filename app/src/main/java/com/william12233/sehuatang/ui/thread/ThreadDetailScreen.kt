package com.william12233.sehuatang.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.william12233.sehuatang.data.model.ContentBlock
import com.william12233.sehuatang.data.model.ForumAttachment
import com.william12233.sehuatang.data.model.ForumPost
import com.william12233.sehuatang.data.model.ForumThreadPage
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.AttachmentCard
import com.william12233.sehuatang.ui.components.LoadingThreadSkeleton
import com.william12233.sehuatang.ui.components.NativeFailureCard
import com.william12233.sehuatang.ui.components.NativeLink

@Composable
fun ThreadDetailScreen(
    state: AsyncState<ForumThreadPage>,
    onRetry: () -> Unit,
    onReply: (String) -> Unit,
    onVerification: () -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onDownload: (ForumAttachment) -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDownloadImage: (String) -> Unit = {},
) {
    when (state) {
        AsyncState.Idle, AsyncState.Loading -> LoadingThreadSkeleton(Modifier.padding(18.dp))
        is AsyncState.Error -> NativeFailureCard(
            modifier = Modifier.padding(18.dp),
            failure = state.failure,
            onRetry = onRetry,
            onVerification = if (state.failure is com.william12233.sehuatang.data.model.ForumFailure.Verification) {
                onVerification
            } else null,
        )

        is AsyncState.Data -> ThreadContent(
            page = state.value,
            onReply = onReply,
            onOpenLink = onOpenLink,
            onOpenPage = onOpenPage,
            onShareLink = onShareLink,
            onDownload = onDownload,
            isBookmarked = isBookmarked,
            onToggleBookmark = onToggleBookmark,
            onDownloadImage = onDownloadImage,
        )
    }
}

@Composable
private fun ThreadContent(
    page: ForumThreadPage,
    onReply: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onDownload: (ForumAttachment) -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDownloadImage: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                page.thread.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                listOfNotNull(
                    page.thread.author.takeIf { it.isNotBlank() },
                    page.thread.createdAt.takeIf { it.isNotBlank() },
                    page.thread.replies?.let { "$it 則回覆" },
                ).joinToString("  ·  ").ifBlank { "論壇文章" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onReply(page.sourceUrl) }) {
                    Icon(Icons.Outlined.Reply, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("回覆文章")
                }
                OutlinedButton(onClick = { onShareLink(page.sourceUrl) }) {
                    Icon(Icons.Outlined.Link, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("分享連結")
                }
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        if (isBookmarked) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isBookmarked) "移除收藏" else "加入收藏",
                    )
                }
            }
        }
        items(page.posts, key = { it.id }) { post ->
            PostCard(post, onOpenLink, onDownload, onDownloadImage)
        }
        if (page.nextPageUrl != null || page.previousPageUrl != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        page.previousPageUrl?.let { url ->
                            TextButton(onClick = { onOpenPage(url) }) { Text("上一頁") }
                        } ?: Spacer(Modifier.size(1.dp))
                        Text("第 ${page.page} / ${page.totalPages} 頁", style = MaterialTheme.typography.labelMedium)
                        page.nextPageUrl?.let { url ->
                            TextButton(onClick = { onOpenPage(url) }) {
                                Text("下一頁")
                                Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                            }
                        } ?: Spacer(Modifier.size(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCard(
    post: ForumPost,
    onOpenLink: (String) -> Unit,
    onDownload: (ForumAttachment) -> Unit,
    onDownloadImage: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!post.authorAvatar.isNullOrBlank()) {
                    AsyncImage(
                        model = post.authorAvatar,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(42.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.author, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(post.floor.takeIf { it.isNotBlank() }, post.createdAt.takeIf { it.isNotBlank() })
                            .joinToString("  ·  ")
                            .ifBlank { "論壇會員" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (post.floor.isNotBlank()) Text(post.floor, style = MaterialTheme.typography.labelSmall)
            }
            post.quotedPost?.let { quote ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("引用${quote.author?.let { " · $it" }.orEmpty()}", fontWeight = FontWeight.Medium)
                        Text(quote.text, style = MaterialTheme.typography.bodySmall, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            post.contentBlocks.forEach { block ->
                ContentBlockView(block, onOpenLink, onDownload, onDownloadImage)
            }
            post.editInformation?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ContentBlockView(
    block: ContentBlock,
    onOpenLink: (String) -> Unit,
    onDownload: (ForumAttachment) -> Unit,
    onDownloadImage: (String) -> Unit,
) {
    when (block) {
        is ContentBlock.Text -> Text(block.value, style = MaterialTheme.typography.bodyLarge)
        is ContentBlock.Image -> ForumImage(block.url, block.alt, onDownloadImage)
        is ContentBlock.Link -> NativeLink(block.label) { onOpenLink(block.url) }
        is ContentBlock.Quote -> Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("引用${block.author?.let { " · $it" }.orEmpty()}", fontWeight = FontWeight.Medium)
                block.blocks.forEach { nested -> ContentBlockView(nested, onOpenLink, onDownload, onDownloadImage) }
            }
        }
        is ContentBlock.Code -> Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.inverseSurface,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface)
                Text(
                    block.code,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
        is ContentBlock.AttachmentBlock -> AttachmentCard(block.attachment) { onDownload(block.attachment) }
        is ContentBlock.Video -> Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            NativeLink("開啟影片連結") { onOpenLink(block.url) }
        }
        is ContentBlock.Unsupported -> Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                Text("未支援內容：${block.reason}")
            }
        }
    }
}

@Composable
private fun ForumImage(url: String, alt: String, onDownloadImage: (String) -> Unit) {
    var showFullScreen by remember { mutableStateOf(false) }
    AsyncImage(
        model = url,
        contentDescription = alt.ifBlank { "文章圖片" },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { showFullScreen = true },
        contentScale = ContentScale.Fit,
    )
    if (showFullScreen) FullScreenImage(url, alt, onDownloadImage) { showFullScreen = false }
}

@Composable
private fun FullScreenImage(url: String, alt: String, onDownloadImage: (String) -> Unit, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f) }
                },
        ) {
            AsyncImage(
                model = url,
                contentDescription = alt.ifBlank { "全螢幕圖片" },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                contentScale = ContentScale.Fit,
            )
            IconButton(modifier = Modifier.align(Alignment.TopEnd), onClick = onDismiss) {
                Text("關閉", color = androidx.compose.ui.graphics.Color.White)
            }
            TextButton(
                modifier = Modifier.align(Alignment.BottomCenter),
                onClick = { onDownloadImage(url) },
            ) { Text("儲存圖片", color = androidx.compose.ui.graphics.Color.White) }
        }
    }
}
