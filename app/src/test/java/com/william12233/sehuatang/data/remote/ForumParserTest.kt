package com.william12233.sehuatang.data.remote

import com.william12233.sehuatang.data.model.ContentBlock
import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.data.model.ForumResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumParserTest {
    @Test
    fun capturedSiteVerificationPageIsNotParsedAsForumContent() {
        val html = javaClass.getResource("/fixtures/site-verification.html")!!.readText()
        val result = ForumIndexParser.parse(html, "https://www.sehuatang.org/forum.php")

        assertTrue(result is ForumResult.Failure)
        val failure = (result as ForumResult.Failure).error
        assertTrue(failure is ForumFailure.Verification)
    }

    @Test
    fun discuzStructureIsConvertedToThreadAndSectionModels() {
        val html = """
            <html><head><title>論壇</title></head><body>
              <div class="fl_g">
                <h2>討論區</h2>
                <a href="forum.php?mod=forumdisplay&amp;fid=12">技術交流</a>
                <p class="desc">測試結構，不是正式內容</p>
              </div>
              <ul class="threadlist">
                <li class="stick">
                  <a class="s xst" href="forum.php?mod=viewthread&amp;tid=99&amp;fid=12">HTML Entity &amp; 解析</a>
                  <span class="author">作者</span><span class="num">12 345</span>
                </li>
              </ul>
            </body></html>
        """.trimIndent()

        val result = ForumIndexParser.parse(html, "https://www.sehuatang.org/forum.php")
        assertTrue(result is ForumResult.Success)
        val home = (result as ForumResult.Success).value
        assertEquals(12L, home.sections.single().id)
        assertEquals("技術交流", home.sections.single().name)
        assertEquals(99L, home.latestThreads.single().id)
        assertEquals("HTML Entity & 解析", home.latestThreads.single().title)
        assertTrue(home.latestThreads.single().isPinned)
    }

    @Test
    fun threadPostsKeepImagesLinksQuotesAndAttachmentsStructured() {
        val html = """
            <html><head><title>測試文章</title></head><body>
              <h1 id="thread_subject">文章標題</h1>
              <div id="post_101" class="plhin">
                <div class="authi"><a href="home.php?mod=space&amp;uid=7">作者</a><em>2026-10-18</em></div>
                <div class="pcb"><p>正文文字</p><blockquote><cite>引用者</cite>引用內容</blockquote>
                  <img src="static/image/demo.jpg" alt="圖片" />
                  <a href="https://example.test/read">超連結</a>
                  <a class="attach" href="attachment.php?aid=8">附件.zip</a>
                </div>
              </div>
            </body></html>
        """.trimIndent()

        val result = ThreadDetailParser.parse(
            html,
            "https://www.sehuatang.org/forum.php?mod=viewthread&tid=99",
        )
        assertTrue(result is ForumResult.Success)
        val post = (result as ForumResult.Success).value.posts.single()
        assertEquals("作者", post.author)
        assertTrue(post.contentBlocks.any { it is ContentBlock.Text && it.value.contains("正文文字") })
        assertTrue(post.contentBlocks.any { it is ContentBlock.Image && it.url.endsWith("demo.jpg") })
        assertTrue(post.contentBlocks.any { it is ContentBlock.Link && it.url == "https://example.test/read" })
        assertEquals("附件.zip", post.attachments.single().title)
        assertFalse(post.contentBlocks.any { it is ContentBlock.Unsupported && it.text.contains("正文") })
    }

    @Test
    fun loginAndReplyFormsUseNamesFromReturnedHtml() {
        val login = LoginParser.parse(
            """
            <form action="member.php?mod=logging&amp;action=login" method="post">
              <input type="hidden" name="formhash" value="abc" />
              <input type="text" name="custom_user" />
              <input type="password" name="custom_pass" />
              <input type="checkbox" name="cookietime" value="2592000" />
            </form>
            """.trimIndent(),
            "https://www.sehuatang.org/member.php?mod=logging&action=login",
        )
        assertTrue(login is ForumResult.Success)
        val form = (login as ForumResult.Success).value
        assertEquals("custom_user", form.usernameField)
        assertEquals("custom_pass", form.passwordField)
        assertEquals("abc", form.hiddenFields["formhash"])

        val post = PostFormParser.parse(
            """
            <form action="forum.php?mod=post&amp;action=reply&amp;fid=12" method="post">
              <input type="hidden" name="formhash" value="xyz" />
              <textarea name="message_body"></textarea>
              <input type="text" name="subject_line" />
            </form>
            """.trimIndent(),
            "https://www.sehuatang.org/forum.php?mod=viewthread&tid=99",
        )
        assertTrue(post is ForumResult.Success)
        val postForm = (post as ForumResult.Success).value
        assertEquals("message_body", postForm.contentField)
        assertEquals("subject_line", postForm.titleField)
        assertEquals("xyz", postForm.hiddenFields["formhash"])
    }
}
