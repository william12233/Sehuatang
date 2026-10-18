package com.william12233.sehuatang.web.navigation

/** 論壇公開頁面的集中入口；不把不存在的 API 當成資料來源。 */
object ForumUrls {
    const val BASE = "https://www.sehuatang.org/"
    const val HOME = "${BASE}forum.php"
    const val LOGIN = "${BASE}member.php?mod=logging&action=login"
    const val SEARCH = "${BASE}search.php?mod=forum"
    const val MESSAGES = "${BASE}home.php?mod=space&do=pm"
    const val NOTICES = "${BASE}home.php?mod=space&do=notice"
    const val MEMBER_HOME = "${BASE}home.php?mod=space"
    const val MY_GUIDE = "${BASE}forum.php?mod=guide&view=my"
    const val SIGN_IN = "${BASE}plugin.php?id=dd_sign:index"

    fun forumDisplay(forumId: Long, page: Int = 1): String =
        "${BASE}forum.php?mod=forumdisplay&fid=$forumId&page=${page.coerceAtLeast(1)}"

    fun viewThread(threadId: Long, page: Int = 1): String =
        "${BASE}forum.php?mod=viewthread&tid=$threadId&page=${page.coerceAtLeast(1)}"
}
