package com.william12233.sehuatang.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.william12233.sehuatang.data.model.RecentVisit
import com.william12233.sehuatang.web.navigation.ForumUrls
import com.william12233.sehuatang.web.session.WebTab
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeShowsThirdPartyDisclaimerAndOpensForum() {
        var openedTab: WebTab? = null
        var openedUrl: String? = null
        composeRule.setContent {
            HomeScreen(
                recentVisits = listOf(
                    RecentVisit("測試文章", "https://www.sehuatang.org/forum.php?tid=1", 1L),
                ),
                onOpenWeb = { tab, url ->
                    openedTab = tab
                    openedUrl = url
                },
            )
        }

        composeRule.onNodeWithText("第三方客戶端，非色花堂官方產品").assertIsDisplayed()
        composeRule.onNodeWithText("開啟論壇").performClick()
        assertEquals(WebTab.FORUM, openedTab)
        assertEquals(ForumUrls.HOME, openedUrl)
    }
}
