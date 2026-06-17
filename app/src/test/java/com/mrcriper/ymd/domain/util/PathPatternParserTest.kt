package com.mrcriper.ymd.domain.util

import com.mrcriper.ymd.domain.model.Album
import com.mrcriper.ymd.domain.model.Artist
import com.mrcriper.ymd.domain.model.PathPattern
import com.mrcriper.ymd.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathPatternParserTest {

    private fun sample(
        title: String = "Song",
        version: String? = null,
        albumTitle: String = "Album",
        artistName: String = "Band",
        trackIds: List<String> = listOf("t1", "t2", "t3"),
    ): Track {
        val artist = Artist(id = "ar1", name = artistName)
        val album = Album(id = "a1", title = albumTitle, trackIds = trackIds, artists = listOf(artist))
        return Track(
            id = "t1",
            title = title,
            version = version,
            albums = listOf(album),
            artists = listOf(artist),
        )
    }

    @Test fun `replaces basic placeholders`() {
        val out = PathPatternParser.parse(PathPattern("#album-artist/#album/#number - #title"), sample())
        assertEquals("Band/Album/1 - Song", out)
    }

    @Test fun `appends version in parens`() {
        val out = PathPatternParser.parse(PathPattern("#title"), sample(title = "T", version = "Remastered"))
        assertEquals("T (Remastered)", out)
    }

    @Test fun `pads number with leading zeros`() {
        val t = sample(trackIds = listOf("t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9", "t10", "t11", "t12", "t13", "t14", "t15", "t16", "t17", "t18", "t19", "t20", "t21", "t22", "t23", "t24", "t25", "t26", "t27", "t28", "t29", "t30", "t31", "t32", "t33", "t34", "t35", "t36", "t37", "t38", "t39", "t40", "t41", "t42", "t43", "t44", "t45", "t46", "t47", "t48", "t49", "t50", "t51", "t52", "t53", "t54", "t55", "t56", "t57", "t58", "t59", "t60", "t61", "t62", "t63", "t64", "t65", "t66", "t67", "t68", "t69", "t70", "t71", "t72", "t73", "t74", "t75", "t76", "t77", "t78", "t79", "t80", "t81", "t82", "t83", "t84", "t85", "t86", "t87", "t88", "t89", "t90", "t91", "t92", "t93", "t94", "t95", "t96", "t97", "t98", "t99", "t100"))
        val out = PathPatternParser.parse(PathPattern("#number-padded"), t)
        // width = 3 → "001"
        assertEquals("001", out)
    }

    @Test fun `safe mode sanitizes slashes`() {
        val out = PathPatternParser.parse(PathPattern("#title"), sample(title = "A/B\\C"))
        assertTrue("Safe mode must not contain '/'", !out.contains('/'))
        assertTrue("Safe mode must not contain '\\\\'", !out.contains('\\'))
    }

    @Test fun `unsafe mode preserves characters except path separators`() {
        val t = sample(title = "Hello: World! ()")
        val out = PathPatternParser.parse(PathPattern("#title", unsafe = true), t)
        assertEquals("Hello: World! ()", out)
    }

    @Test fun `empty fields collapse gracefully`() {
        val out = PathPatternParser.parse(
            PathPattern("#album-artist/#album/#number - #title"),
            Track(id = "t1", title = "Lonely", artists = emptyList(), albums = emptyList()),
        )
        // No album → no track number, no artist names → empty placeholders remain empty
        assertEquals("// - Lonely", out)
    }
}
