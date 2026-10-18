package com.william12233.sehuatang.web.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeUrlPolicyTest {
    @Test
    fun trustedForumUrlCanBePersisted() {
        assertTrue(SafeUrlPolicy.canPersistInLocalHistory("https://www.sehuatang.org/forum.php?mod=viewthread&tid=123"))
    }

    @Test
    fun loginAndCredentialLikeUrlsAreNeverPersisted() {
        assertFalse(SafeUrlPolicy.canPersistInLocalHistory(ForumUrls.LOGIN))
        assertFalse(
            SafeUrlPolicy.canPersistInLocalHistory(
                "https://www.sehuatang.org/forum.php?token=temporary-value",
            ),
        )
    }

    @Test
    fun unknownHostIsWebCompatibleButNotTrusted() {
        val url = "https://download.example.test/file.zip"
        assertTrue(SafeUrlPolicy.isHttpUrl(url))
        assertFalse(SafeUrlPolicy.isTrustedForumHost(url))
        assertFalse(SafeUrlPolicy.canPersistInLocalHistory(url))
    }

    @Test
    fun nonHttpSchemesAreRejected() {
        assertFalse(SafeUrlPolicy.isHttpUrl("javascript:alert(1)"))
        assertFalse(SafeUrlPolicy.isHttpUrl("file:///sdcard/file"))
    }
}
