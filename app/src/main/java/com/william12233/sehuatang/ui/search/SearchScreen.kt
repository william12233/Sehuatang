package com.william12233.sehuatang.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.web.navigation.ForumUrls
import com.william12233.sehuatang.web.session.WebTab

@Composable
fun SearchScreen(onOpenWeb: (WebTab, String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text("論壇搜尋", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "搜尋欄位與驗證規則由論壇原始頁面提供。開啟後可使用網站的完整搜尋、分頁與權限流程。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onOpenWeb(WebTab.SEARCH, ForumUrls.SEARCH) }) {
            Text("開啟網站搜尋")
        }
    }
}
