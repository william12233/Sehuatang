package com.william12233.sehuatang.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.william12233.sehuatang.data.model.ForumHome
import com.william12233.sehuatang.data.model.ForumThread
import com.william12233.sehuatang.ui.AsyncState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nativeHomeRendersRepositoryThreadModel() {
        val thread = ForumThread(
            id = 42L,
            title = "測試文章",
            author = "測試會員",
            url = "https://www.sehuatang.org/forum.php?mod=viewthread&tid=42",
        )
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    home = AsyncState.Data(
                        ForumHome(
                            sections = emptyList(),
                            latestThreads = listOf(thread),
                            popularThreads = emptyList(),
                            sourceUrl = "https://www.sehuatang.org/forum.php",
                        ),
                    ),
                    recentVisits = emptyList(),
                    onRefresh = {},
                    onOpenSection = {},
                    onOpenThread = {},
                    onOpenRecent = {},
                    onOpenLogin = {},
                    onOpenVerification = {},
                )
            }
        }
        composeRule.onNodeWithText("測試文章").assertIsDisplayed()
    }
}
