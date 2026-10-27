package com.occamzchainsaw.straightscorer.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.occamzchainsaw.straightscorer.data.rules.DocBlock
import com.occamzchainsaw.straightscorer.data.rules.RichText

/** Search hits for one segment; [isCurrent] marks the one the user stepped to. */
@Immutable
data class Highlight(val range: IntRange, val isCurrent: Boolean)

/** Everything a block needs to draw links and search highlights. */
@Immutable
class BlockRenderContext(
    val onLink: (String) -> Unit,
    /** Highlights for the segment at the given index within the current block. */
    val highlights: (segmentIndex: Int) -> List<Highlight>
)

private val TableColumnWidth = 176.dp

@Composable
fun MarkdownBlock(
    block: DocBlock,
    context: BlockRenderContext,
    modifier: Modifier = Modifier,
    /** Inside list items: no extra spacing, so the text lines up with the marker. */
    compact: Boolean = false
) {
    when (block) {
        is DocBlock.Heading -> HeadingBlock(block, context, modifier)
        is DocBlock.Paragraph -> RichTextView(
            block.content,
            context,
            segmentIndex = 0,
            style = bodyStyle(),
            modifier = modifier.padding(vertical = if (compact) 0.dp else 6.dp)
        )
        is DocBlock.Caption -> RichTextView(
            block.content,
            context,
            segmentIndex = 0,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        is DocBlock.ListBlock -> ListView(block, context, segmentOffset = 0, modifier = modifier.padding(vertical = 4.dp))
        is DocBlock.Table -> TableView(block, context, modifier.padding(vertical = 12.dp))
        DocBlock.Divider -> HorizontalDivider(modifier.padding(vertical = 16.dp))
    }
}

@Composable
private fun bodyStyle(): TextStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)

@Composable
private fun HeadingBlock(block: DocBlock.Heading, context: BlockRenderContext, modifier: Modifier) {
    val typography = MaterialTheme.typography
    val (style, topPadding) = when (block.level) {
        1 -> typography.headlineSmall.copy(fontWeight = FontWeight.Bold) to 32.dp
        2 -> typography.titleLarge to 24.dp
        else -> typography.titleMedium.copy(fontWeight = FontWeight.SemiBold) to 20.dp
    }
    Column(modifier.padding(top = topPadding, bottom = 8.dp)) {
        RichTextView(
            block.content,
            context,
            segmentIndex = 0,
            style = style,
            color = if (block.level == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (block.level == 1) {
            HorizontalDivider(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(48.dp),
                thickness = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ListView(block: DocBlock.ListBlock, context: BlockRenderContext, segmentOffset: Int, modifier: Modifier = Modifier) {
    var offset = segmentOffset
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        block.items.forEach { item ->
            Row {
                if (block.ordered) {
                    Text(
                        text = item.marker,
                        style = bodyStyle(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(32.dp)
                    )
                } else {
                    // centred on the first line of text (26sp line height)
                    Box(Modifier.width(22.dp).height(26.dp), contentAlignment = Alignment.CenterStart) {
                        Box(
                            Modifier
                                .padding(start = 4.dp)
                                .size(6.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    item.blocks.forEach { child ->
                        val childOffset = offset
                        when (child) {
                            is DocBlock.ListBlock -> ListView(child, context, childOffset, Modifier.padding(top = 6.dp))
                            else -> {
                                val shifted = BlockRenderContext(context.onLink) { context.highlights(childOffset + it) }
                                MarkdownBlock(child, shifted, compact = true)
                            }
                        }
                        offset += child.segments.size
                    }
                }
            }
        }
    }
}

@Composable
private fun TableView(table: DocBlock.Table, context: BlockRenderContext, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            var segment = 0
            TableRow(
                cells = table.header,
                context = context,
                firstSegment = segment,
                background = scheme.surfaceContainerHigh,
                isHeader = true
            )
            segment += table.header.size
            table.rows.forEach { row ->
                HorizontalDivider(color = scheme.outlineVariant, modifier = Modifier.width(TableColumnWidth * row.size))
                TableRow(cells = row, context = context, firstSegment = segment)
                segment += row.size
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<RichText>,
    context: BlockRenderContext,
    firstSegment: Int,
    background: Color = Color.Transparent,
    isHeader: Boolean = false
) {
    Surface(color = background) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            cells.forEachIndexed { index, cell ->
                RichTextView(
                    cell,
                    context,
                    segmentIndex = firstSegment + index,
                    style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .width(TableColumnWidth)
                        .fillMaxHeight()
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun RichTextView(
    content: RichText,
    context: BlockRenderContext,
    segmentIndex: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = content.toAnnotatedString(context.highlights(segmentIndex), context.onLink),
        style = style,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun RichText.toAnnotatedString(highlights: List<Highlight>, onLink: (String) -> Unit): AnnotatedString {
    val scheme = MaterialTheme.colorScheme
    val linkStyle = TextLinkStyles(SpanStyle(color = scheme.primary, textDecoration = TextDecoration.Underline))
    val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = scheme.surfaceContainerHighest)
    val matchStyle = SpanStyle(background = scheme.tertiaryContainer, color = scheme.onTertiaryContainer)
    val currentStyle = SpanStyle(background = scheme.tertiary, color = scheme.onTertiary)

    return buildAnnotatedString {
        append(text)
        spans.forEach { span ->
            when (span) {
                is RichText.Bold -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), span.start, span.end)
                is RichText.Italic -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), span.start, span.end)
                is RichText.Code -> addStyle(codeStyle, span.start, span.end)
                is RichText.Superscript -> addStyle(
                    SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.7.em),
                    span.start,
                    span.end
                )
                is RichText.Link -> addLink(
                    LinkAnnotation.Clickable(tag = span.url, styles = linkStyle) { onLink(span.url) },
                    span.start,
                    span.end
                )
            }
        }
        highlights.forEach { highlight ->
            val end = (highlight.range.last + 1).coerceAtMost(text.length)
            if (highlight.range.first < end) {
                addStyle(if (highlight.isCurrent) currentStyle else matchStyle, highlight.range.first, end)
            }
        }
    }
}
