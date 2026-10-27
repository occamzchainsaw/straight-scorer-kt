package com.occamzchainsaw.straightscorer.data.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RulesDocumentTest {

    private val rules by lazy { RulesDocument.parse(File("src/main/assets/rules.md").readText()) }

    private fun links(document: RulesDocument): List<String> = document.blocks
        .flatMap { block -> block.segments }
        .flatMap { segment -> segment.spans.filterIsInstance<RichText.Link>().map { it.url } }

    @Test
    fun everyInternalLinkInTheRules_resolvesToAHeading() {
        val internal = links(rules).filter { it.startsWith("#") }
        assertTrue("expected the table of contents links", internal.size > 30)

        val broken = internal.filter { rules.resolveAnchor(it) == null }
        assertEquals("unresolved links", emptyList<String>(), broken)
    }

    @Test
    fun linksJumpToTheMatchingHeading() {
        val index = rules.resolveAnchor("#breaking-foul")!!
        val heading = rules.blocks[index] as DocBlock.Heading
        assertEquals("11 Breaking Foul", heading.content.text)

        val nested = rules.blocks[rules.resolveAnchor("#double-hit--frozen-balls")!!] as DocBlock.Heading
        assertEquals("10.6 Double Hit / Frozen Balls", nested.content.text)
    }

    @Test
    fun tableOfContents_listsHeadingsButNotItself() {
        val titles = rules.tableOfContents.map { it.title }
        assertTrue(titles.first().startsWith("1."))
        assertTrue("Table of Contents" !in titles)
        assertTrue("12.1 Three Consecutive Fouls" in titles)
    }

    @Test
    fun racksTable_isParsedWithLineBreaksAndSuperscripts() {
        val table = rules.blocks.filterIsInstance<DocBlock.Table>().single()
        assertEquals(4, table.header.size)
        assertEquals(5, table.rows.size)

        val cell = table.rows[0][1]
        assertEquals("15th Ball: Foot Spot\nCue-ball: above Head String", cell.text)
        assertTrue(cell.spans.contains(RichText.Superscript(2, 4)))

        val rowTitle = table.rows[0][0]
        assertEquals("15th Ball in the Rack", rowTitle.text)
        assertTrue(rowTitle.spans.any { it is RichText.Bold && it.start == 0 && it.end == rowTitle.text.length })
    }

    @Test
    fun centeredCaption_isParsed() {
        val caption = rules.blocks.filterIsInstance<DocBlock.Caption>().single()
        assertEquals("Table 1. Summary of the Rules for 14.1 Racking Situations", caption.content.text)
    }

    @Test
    fun orderedListsKeepTheirNumbers() {
        val list = rules.blocks.filterIsInstance<DocBlock.ListBlock>().first { it.ordered }
        assertEquals("1.", list.items.first().marker)
    }

    @Test
    fun parsesInlineFormatting() {
        val document = RulesDocument.parse("Some **bold**, *italic*, `code` and [a link](#x).")
        val paragraph = document.blocks.single() as DocBlock.Paragraph
        assertEquals("Some bold, italic, code and a link.", paragraph.content.text)
        assertTrue(RichText.Bold(5, 9) in paragraph.content.spans)
        assertTrue(RichText.Italic(11, 17) in paragraph.content.spans)
        assertTrue(RichText.Code(19, 23) in paragraph.content.spans)
        assertTrue(RichText.Link(28, 34, "#x") in paragraph.content.spans)
    }

    @Test
    fun search_findsCaseInsensitiveMatchesInEveryKindOfBlock() {
        val matches = rules.search("head spot")
        assertTrue(matches.size > 5)
        assertTrue(matches.any { rules.blocks[it.blockIndex] is DocBlock.Table })

        val first = matches.first()
        val text = rules.blocks[first.blockIndex].segments[first.segmentIndex].text
        assertEquals("head spot", text.substring(first.range).lowercase())
    }

    @Test
    fun search_ignoresVeryShortQueries() {
        assertTrue(rules.search("a").isEmpty())
        assertNotNull(rules.search("foul").firstOrNull())
    }

    @Test
    fun slugs_matchGitHubStyle() {
        assertEquals("double-hit--frozen-balls", RulesDocument.slugify("Double Hit / Frozen Balls"))
        assertTrue("continuous-pool-straight-pool" in RulesDocument.anchorsFor("1. 14.1 Continuous Pool (Straight Pool)"))
        assertTrue("cue-ball-scratch-or-off-the-table" in RulesDocument.anchorsFor("10.1 Cue-ball Scratch or off the Table."))
    }
}
