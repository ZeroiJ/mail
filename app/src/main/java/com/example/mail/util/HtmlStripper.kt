package com.example.mail.util

import androidx.core.text.HtmlCompat

/**
 * Converts raw email HTML into Reader-Mode plain text. Uses Android's
 * HtmlCompat for tag-to-text conversion and regex passes for the structural
 * cruft HtmlCompat alone renders verbatim: full <table> subtrees, CSS blocks,
 * HTML comments and inline style declarations.
 */
object HtmlStripper {

    private val tableBlock = Regex("<table[^>]*>[\\s\\S]*?</table>", RegexOption.IGNORE_CASE)
    private val styleBlock = Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE)
    private val scriptBlock = Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE)
    private val headBlock = Regex("<head[^>]*>[\\s\\S]*?</head>", RegexOption.IGNORE_CASE)
    private val htmlComment = Regex("<!--[\\s\\S]*?-->")
    private val inlineStyle = Regex("\\sstyle\\s*=\\s*\"[^\"]*\"", RegexOption.IGNORE_CASE)

    fun strip(html: String): String {
        var text = html

        // Remove nested tables innermost-first so balanced <table> subtrees
        // (the marketing/template layout) disappear entirely instead of
        // leaking their inner text through HtmlCompat.
        while (true) {
            val match = tableBlock.find(text) ?: break
            text = text.replaceRange(match.range, " ")
        }
        text = text
            .replace(styleBlock, "")
            .replace(scriptBlock, "")
            .replace(headBlock, "")
            .replace(htmlComment, "")
            .replace(inlineStyle, " ")

        val converted = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()

        return converted
            .replace("\u00a0", " ")
            .replace(Regex("[ \\t\\f]+"), " ")
            .replace(Regex(" *\r?\n *"), "\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}