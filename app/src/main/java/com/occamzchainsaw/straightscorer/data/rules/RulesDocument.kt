package com.occamzchainsaw.straightscorer.data.rules

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

/** Inline text with formatting ranges - kept free of Compose so the parser is plain, testable Kotlin. */
data class RichText(val text: String, val spans: List<Span> = emptyList()) {
    sealed interface Span {
        val start: Int
        val end: Int
    }

    data class Bold(override val start: Int, override val end: Int) : Span
    data class Italic(override val start: Int, override val end: Int) : Span
    data class Code(override val start: Int, override val end: Int) : Span
    data class Superscript(override val start: Int, override val end: Int) : Span
    data class Link(override val start: Int, override val end: Int, val url: String) : Span
}

sealed interface DocBlock {
    /** Every piece of searchable text in this block, in rendering order. */
    val segments: List<RichText>

    data class Heading(val level: Int, val content: RichText, val anchors: Set<String>) : DocBlock {
        override val segments get() = listOf(content)
    }

    data class Paragraph(val content: RichText) : DocBlock {
        override val segments get() = listOf(content)
    }

    /** `<center>` blocks, e.g. table captions. */
    data class Caption(val content: RichText) : DocBlock {
        override val segments get() = listOf(content)
    }

    data class ListBlock(val ordered: Boolean, val items: List<ListItem>) : DocBlock {
        override val segments get() = items.flatMap { item -> item.blocks.flatMap { it.segments } }
    }

    data class ListItem(val marker: String, val blocks: List<DocBlock>)

    data class Table(val header: List<RichText>, val rows: List<List<RichText>>) : DocBlock {
        override val segments get() = header + rows.flatten()
    }

    data object Divider : DocBlock {
        override val segments get() = emptyList<RichText>()
    }
}

/** A search hit: [range] inside segment [segmentIndex] of block [blockIndex]. */
data class SearchMatch(val blockIndex: Int, val segmentIndex: Int, val range: IntRange)

data class TocEntry(val level: Int, val title: String, val blockIndex: Int)

class RulesDocument(val blocks: List<DocBlock>) {

    private val anchorIndex: Map<String, Int> = buildMap {
        blocks.forEachIndexed { index, block ->
            if (block is DocBlock.Heading) block.anchors.forEach { putIfAbsent(it, index) }
        }
    }

    /** Headings for the contents sheet; the document's own "Table of Contents" heading is left out. */
    val tableOfContents: List<TocEntry> = blocks.mapIndexedNotNull { index, block ->
        (block as? DocBlock.Heading)
            ?.takeUnless { it.content.text.equals("Table of Contents", ignoreCase = true) }
            ?.let { TocEntry(it.level, it.content.text, index) }
    }

    /** Block index for a link like `#breaking-foul`, or null if nothing matches. */
    fun resolveAnchor(link: String): Int? {
        val anchor = link.removePrefix("#").lowercase()
        return anchorIndex[anchor] ?: anchorIndex[slugify(anchor)]
    }

    /** Case-insensitive search across all text. */
    fun search(query: String): List<SearchMatch> {
        val needle = query.trim()
        if (needle.length < 2) return emptyList()
        return buildList {
            blocks.forEachIndexed { blockIndex, block ->
                block.segments.forEachIndexed { segmentIndex, segment ->
                    var from = 0
                    while (true) {
                        val start = segment.text.indexOf(needle, from, ignoreCase = true)
                        if (start < 0) break
                        add(SearchMatch(blockIndex, segmentIndex, start until start + needle.length))
                        from = start + needle.length
                    }
                }
            }
        }
    }

    companion object {
        fun parse(markdown: String): RulesDocument {
            val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(markdown)
            return RulesDocument(MarkdownConverter(markdown).blocks(root.children))
        }

        /** GitHub-style heading slug: lowercase, punctuation dropped, spaces to hyphens. */
        fun slugify(text: String): String = text.trim().lowercase()
            .filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
            .replace(' ', '-')

        private val leadingNumbers = Regex("""^(\d+(\.\d+)*\.?\s+)+""")

        /** Anchors a heading answers to: its full slug and the slug without the section number ("10.1 ..."). */
        fun anchorsFor(title: String): Set<String> =
            setOf(slugify(title), slugify(title.replace(leadingNumbers, "")))
    }
}

private class MarkdownConverter(private val source: String) {

    fun blocks(nodes: List<ASTNode>): List<DocBlock> = nodes.mapNotNull(::block)

    private fun block(node: ASTNode): DocBlock? = when (node.type) {
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> heading(node, 1)
        MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> heading(node, 2)
        MarkdownElementTypes.ATX_3 -> heading(node, 3)
        MarkdownElementTypes.ATX_4 -> heading(node, 4)
        MarkdownElementTypes.ATX_5 -> heading(node, 5)
        MarkdownElementTypes.ATX_6 -> heading(node, 6)
        MarkdownElementTypes.PARAGRAPH -> inline(node.children).takeIf { it.text.isNotBlank() }?.let(DocBlock::Paragraph)
        MarkdownElementTypes.ORDERED_LIST -> list(node, ordered = true)
        MarkdownElementTypes.UNORDERED_LIST -> list(node, ordered = false)
        GFMElementTypes.TABLE -> table(node)
        MarkdownElementTypes.HTML_BLOCK -> htmlBlock(node)
        MarkdownTokenTypes.HORIZONTAL_RULE -> DocBlock.Divider
        MarkdownElementTypes.BLOCK_QUOTE -> blocks(node.children).firstOrNull()
        else -> null
    }

    private fun heading(node: ASTNode, level: Int): DocBlock.Heading {
        val contentNode = node.children.firstOrNull {
            it.type == MarkdownTokenTypes.ATX_CONTENT || it.type == MarkdownTokenTypes.SETEXT_CONTENT
        }
        val content = inline(contentNode?.children ?: emptyList()).trimmed()
        return DocBlock.Heading(level, content, RulesDocument.anchorsFor(content.text))
    }

    private fun list(node: ASTNode, ordered: Boolean): DocBlock.ListBlock {
        val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }.map { item ->
            val marker = item.children
                .firstOrNull { it.type == MarkdownTokenTypes.LIST_NUMBER || it.type == MarkdownTokenTypes.LIST_BULLET }
                ?.getTextInNode(source)?.toString()?.trim()
                ?: "•"
            DocBlock.ListItem(
                marker = if (ordered) marker else "•",
                blocks = blocks(item.children)
            )
        }
        return DocBlock.ListBlock(ordered, items)
    }

    private fun table(node: ASTNode): DocBlock.Table {
        fun cells(row: ASTNode) = row.children.filter { it.type == GFMTokenTypes.CELL }.map { inline(it.children).trimmed() }
        val header = node.children.firstOrNull { it.type == GFMElementTypes.HEADER }?.let(::cells) ?: emptyList()
        val rows = node.children.filter { it.type == GFMElementTypes.ROW }.map(::cells)
        return DocBlock.Table(header, rows)
    }

    private fun htmlBlock(node: ASTNode): DocBlock? {
        val html = node.getTextInNode(source).toString()
        val text = html.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n").replace(Regex("<[^>]+>"), "").trim()
        if (text.isEmpty()) return null
        val content = withSuperscripts(RichText(text))
        return if (html.contains("<center", ignoreCase = true)) DocBlock.Caption(content) else DocBlock.Paragraph(content)
    }

    /** Builds formatted text from inline nodes. */
    private fun inline(nodes: List<ASTNode>): RichText {
        val builder = StringBuilder()
        val spans = mutableListOf<RichText.Span>()

        fun visit(node: ASTNode) {
            val start = builder.length
            when (node.type) {
                MarkdownElementTypes.STRONG -> {
                    node.children.forEach(::visit)
                    spans += RichText.Bold(start, builder.length)
                }
                MarkdownElementTypes.EMPH -> {
                    node.children.forEach(::visit)
                    spans += RichText.Italic(start, builder.length)
                }
                MarkdownElementTypes.CODE_SPAN -> {
                    node.children.filter { it.type != MarkdownTokenTypes.BACKTICK }.forEach(::visit)
                    spans += RichText.Code(start, builder.length)
                }
                MarkdownElementTypes.INLINE_LINK -> {
                    val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
                    val destination = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }
                        ?.getTextInNode(source)?.toString().orEmpty()
                    label?.children
                        ?.filter { it.type != MarkdownTokenTypes.LBRACKET && it.type != MarkdownTokenTypes.RBRACKET }
                        ?.forEach(::visit)
                    spans += RichText.Link(start, builder.length, destination)
                }
                MarkdownElementTypes.AUTOLINK -> {
                    val url = node.getTextInNode(source).toString().trim('<', '>')
                    builder.append(url)
                    spans += RichText.Link(start, builder.length, url)
                }
                MarkdownTokenTypes.EMPH, MarkdownTokenTypes.BACKTICK -> Unit
                MarkdownTokenTypes.EOL -> builder.append(' ')
                MarkdownTokenTypes.HARD_LINE_BREAK -> builder.append('\n')
                MarkdownTokenTypes.HTML_TAG -> {
                    if (node.getTextInNode(source).matches(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE))) {
                        builder.append('\n')
                    }
                }
                else -> if (node.children.isEmpty()) {
                    builder.append(node.getTextInNode(source))
                } else {
                    node.children.forEach(::visit)
                }
            }
        }

        nodes.forEach(::visit)
        return withSuperscripts(RichText(builder.toString(), spans))
    }

    private fun RichText.trimmed(): RichText {
        val leading = text.length - text.trimStart().length
        val trimmed = text.trim()
        return RichText(
            trimmed,
            spans.mapNotNull { it.shifted(-leading, trimmed.length) }
        )
    }
}

private val superscript = Regex("""\^([^^\s]{1,12})\^""")

/** Pandoc-style `15^th^` superscripts, which the GFM parser leaves as plain text. */
private fun withSuperscripts(rich: RichText): RichText {
    var text = rich.text
    var spans = rich.spans
    while (true) {
        val match = superscript.find(text) ?: break
        val start = match.range.first
        val inner = match.groupValues[1]
        text = text.replaceRange(match.range, inner)
        // the carets at match.range.first and match.range.last are removed
        spans = spans.map { span -> span.adjustedForRemoval(start, match.range.last) } +
            RichText.Superscript(start, start + inner.length)
    }
    return RichText(text, spans)
}

/** Adjusts a span after the characters at [firstCaret] and [secondCaret] (original positions) were removed. */
private fun RichText.Span.adjustedForRemoval(firstCaret: Int, secondCaret: Int): RichText.Span {
    fun shift(position: Int) = position - (if (position > firstCaret) 1 else 0) - (if (position > secondCaret) 1 else 0)
    return copySpan(shift(start), shift(end))
}

private fun RichText.Span.shifted(offset: Int, maxLength: Int): RichText.Span? {
    val newStart = (start + offset).coerceIn(0, maxLength)
    val newEnd = (end + offset).coerceIn(0, maxLength)
    return if (newEnd > newStart) copySpan(newStart, newEnd) else null
}

private fun RichText.Span.copySpan(start: Int, end: Int): RichText.Span = when (this) {
    is RichText.Bold -> copy(start = start, end = end)
    is RichText.Italic -> copy(start = start, end = end)
    is RichText.Code -> copy(start = start, end = end)
    is RichText.Superscript -> copy(start = start, end = end)
    is RichText.Link -> copy(start = start, end = end)
}
