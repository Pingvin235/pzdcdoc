package org.pzdcdoc;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test, using demo samples.
 *
 * @author Shamil Vakhitov
 */
public class DemoTest {
    private static final String SOURCE_DIR = "src/doc";
    private static final String TARGET_DIR = "target/doc-out";

    @BeforeClass
    public static void init() throws Exception {
        FileUtils.deleteQuietly(new File(TARGET_DIR));
        Generator.main(new String[] { "--in", SOURCE_DIR, "--out", TARGET_DIR });
    }

    @Test
    public void checkResources() throws Exception {
        var dir = new File(TARGET_DIR, "_res");
        Assert.assertTrue(dir.exists());
        Assert.assertEquals(
            "Resource files set",
            Set.of(
                "asciidoctor.css", "coderay-asciidoctor.css", "diagram.drawio", "diagram.svg", "eclipse_plugin.png",
                "font.css", "image.png", "jquery-3.3.1.js",
                "lunr-2.3.6.js", "lunr.de.js", "lunr.multi.js", "lunr.ru.js", "lunr.stemmer.support.js",
                "pzdcdoc.css", "pzdcdoc.js", "pzsearch.js", "Snippet.java", "vscode_drawio.png", "vscode_plugin.png"
            ),
            Set.of(dir.list()));
    }

    @Test
    public void checkFileDemo() throws Exception {
        var file = new File(TARGET_DIR, "demo.html");
        Assert.assertTrue("File exists", file.exists());
        Assert.assertEquals("File size", 26279, file.length());

        var doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());
        checkFileDemoHeader(doc);
        checkFileDemoToC(doc);
        checkFileDemoContent(doc);
    }

    private void checkFileDemoHeader(Document doc) {
        var links = doc.select("head link");
        Assert.assertEquals("Links count", 5, links.size());
    }

    private void checkFileDemoToC(Document doc) {
        var tocEmpty = doc.selectFirst("body #header #toc.toc");
        Assert.assertNotNull("ToC empty", tocEmpty);
        Assert.assertEquals("ToC empty children", 0, tocEmpty.childrenSize());

        var tocLeft = doc.selectFirst("body #header #toc.toc2");
        Assert.assertNotNull("ToC left", tocLeft);

        var search = tocLeft.selectFirst("#search");
        Assert.assertNotNull("Search", search);
        Assert.assertNotNull("Search input", search.selectFirst("#search-input input"));
        Assert.assertNotNull("Search count", search.selectFirst("#search-count"));

        Assert.assertEquals("ToC items count", 27, tocLeft.select("li").size());
        Assert.assertEquals("ToC links count", 27, tocLeft.select("a").size());
    }

    private void checkFileDemoContent(Document doc) {
        // TODO: Links count, snippets.
    }
}
