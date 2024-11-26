package org.pzdcdoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Asciidoctor.Factory;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pzdcdoc.processor.JavaDocLink;
import org.pzdcdoc.processor.drawio.DrawIOBlock;
import org.pzdcdoc.processor.drawio.DrawIO;
import org.pzdcdoc.processor.snippet.Snippet;

/**
 * The main runnable class, converting AsciiDoc sources to HTML.
 *
 * @author Shamil Vakhitov
 */
public class Generator {
    private static final Logger log = LogManager.getLogger();

    /** {@link Generator} instance attribute */
    public static final String ATTR_GENERATOR = "pzdc-generator";
    /** Source {@link File} */
    public static final String ATTR_SOURCE = "pzdc-source";
    /** Target {@link Path} */
    public static final String ATTR_TARGET = "pzdc-target";
    /** String with relative for the current file path to the root directory */
    public static final String ATTR_PATH_TO_ROOT = "pzdc-path-to-root";

    private static final String DIR_RES = "_res";

    private static final String EXT_ADOC = ".adoc";
    private static final String EXT_ADOCF = ".adocf";
    private static final String EXT_HTML = ".html";

    private static final String ATTR_SITE_TITLE = "pzdc-site-title";

    private final Asciidoctor asciidoctor = Factory.create();

    private static final String[] SCRIPTS = { "jquery-3.3.1.js", "pzdcdoc.js",
        // https://lunrjs.com/guides/language_support.html
        "lunr-2.3.6.js", "lunr.stemmer.support.js", "lunr.multi.js", "lunr.ru.js", "lunr.de.js"
    };
    private static final String[] SCRIPTS_INJECT = ArrayUtils.add(SCRIPTS, Search.SCRIPT);

    private static final String ASCIIDOCTOR_DEFAULT_CSS = "asciidoctor.css";
    private static final String PZDCDOC_CSS = "pzdcdoc.css";
    private static final String FONT_CSS = "font.css";
    private static final String[] STYLESHEETS = { ASCIIDOCTOR_DEFAULT_CSS, PZDCDOC_CSS, FONT_CSS, "coderay-asciidoctor.css" };
    private static final String[] STYLESHEETS_INJECT = { PZDCDOC_CSS, FONT_CSS };

    /**
     * Entry point for the generator
     * @param args command line arguments
     * @throws Exception all exceptions cause exit with printing out stack trace
     */
    public static void main(String[] args) throws Exception {
        var gen = new Generator();
        var parser = new CmdLineParser(gen);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }

        int errors = gen.process();
        errors += gen.check();

        log.info("DONE!");

        if (errors > 0) {
            log.error("ERRORS => {}", errors);
            System.exit(errors);
        }
    }

    @Option(required = true, name = "-i", aliases = { "--in" }, usage = "Source directory path")
    private File sourceDir;
    @Option(required = true, name = "-o", aliases = { "--out" }, usage = "Target directory path")
    private File targetDir;
    @Option(required = false, name = "-a", aliases = { "--attribute" }, usage = "Attribute name=value")
    private Map<String, String> attributes;

    /** Cached ToC from index.adoc for injecting everywhere. */
    private Element toc;
    /** Search supporting object. */
    private final Search search = new Search();
    /** Processing errors counter. */
    private int errors;

    private Generator() throws Exception {
        // https://github.com/asciidoctor/asciidoctorj/blob/v2.1.0/docs/integrator-guide.adoc
        JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
        javaExtensionRegistry.inlineMacro(new JavaDocLink());
        javaExtensionRegistry.inlineMacro(new DrawIO());
        javaExtensionRegistry.blockMacro(new DrawIOBlock());
        javaExtensionRegistry.block(new Snippet());
        //javaExtensionRegistry.treeprocessor(new Treeprocessor());
    }

    /**
     * Increments error's counter.
     */
    public void error() {
        errors++;
    }

    private int process() throws Exception {
        if (!sourceDir.isDirectory())
            throw new IllegalArgumentException("Incorrect source directory: " + sourceDir);

        process(sourceDir, targetDir, -1, new HashMap<>());
        copyScriptsAndStyles();
        deleteTmpFiles();
        if (errors > 0)
            log.error("PROC ERRORS => {}", errors);
        return errors;
    }

    private int check() throws Exception {
        int errors = new Links().checkDir(targetDir);
        if (errors > 0)
            log.error("CHECK ERRORS => {}", errors);
        return errors;
    }

    private void process(File source, File target, int depth, Map<String, Object> attributes) throws Exception {
        final String sourceName = source.getName();

        // hidden resources, names started by .
        if (sourceName.startsWith(".")) {
            log.debug("Skipping hidden: {}", source);
            return;
        }

        // include - skipping
        if (sourceName.endsWith(EXT_ADOCF)) {
            log.debug("Skipping include: {}", source);
            return;
        }

        if (source.isDirectory()) {
            File[] files = source.listFiles();

            // index.adoc in the root folder must be processed first to be included in all files after
            Arrays.sort(files, (f1, f2) -> {
                if (containsIndex(f1.getName()))
                    return -1;
                if (containsIndex(f2.getName()))
                    return 1;
                return 0;
            });

            attributes = loadAttributes(source, attributes);

            for (File file : files)
                process(file, new File(target, file.getName()), depth + 1, attributes);
        } else {
            if (sourceName.endsWith(EXT_ADOC)) {
                log.info("Processing: {}", source);

                Path targetPath = Paths.get(target.getPath().replace(EXT_ADOC, EXT_HTML));
                String pathToRoot = StringUtils.repeat("../", depth);

                var attrs = Attributes.builder()
                    .backend("html5")
                    .stylesDir(StringUtils.repeat("../", depth) + DIR_RES)
                    .styleSheetName(ASCIIDOCTOR_DEFAULT_CSS)
                    .linkCss(true)
                    .sourceHighlighter("coderay")
                    .icons(Attributes.FONT_ICONS)
                    .tableOfContents(true)
                    .setAnchors(true)
                    .linkAttrs(true)
                    .build();

                attrs.setAttribute("last-update-label", "Powered by <a target='_blank' href='https://pzdcdoc.org'>PzdcDoc</a> at: ");
                attrs.setAttribute(ATTR_SOURCE, source);
                attrs.setAttribute(ATTR_TARGET, targetPath);
                attrs.setAttribute(ATTR_GENERATOR, this);
                attrs.setAttribute(ATTR_PATH_TO_ROOT, pathToRoot);

                attrs.setAttributes(attributes);

                var options = Options.builder()
                    .toFile(false)
                    .safe(SafeMode.UNSAFE)
                    .attributes(attrs)
                    .standalone(true)
                    .build();

                String html = asciidoctor.convertFile(source, options);

                if (toc != null || !extractToC(html, targetPath))
                    html = correctHtmlAndCopyResources(source.toPath(), html, targetPath, pathToRoot, new SourceLink(attributes));

                FileUtils.forceMkdirParent(target);

                try (var writer = FileWriterWithEncoding.builder().setPath(targetPath).setCharset(StandardCharsets.UTF_8).get()) {
                    writer.write(html);
                }

                return;
            }
        }
    }

    private Map<String, Object> loadAttributes(File source, Map<String, Object> attributes) throws DocumentException {
        Path configuration = source.toPath().resolve("pzdcdoc.xml");
        if (configuration.toFile().exists()) {
            log.info("Processing configuration: {}", configuration);
            org.dom4j.Document document = new SAXReader().read(configuration.toFile());

            attributes = new HashMap<>(attributes);

            for (Node attr : document.selectNodes("//attributes/*"))
                attributes.put(attr.getName(), attr.getText());

            if (this.attributes != null)
                attributes.putAll(this.attributes);

            log.info("Read {} attributes", attributes.size());
        }
        return attributes;
    }

    private void copyScriptsAndStyles() throws IOException {
        log.info("Copying scripts and styles.");

        File rootRes = new File(targetDir, DIR_RES);
        if (!rootRes.exists()) rootRes.mkdirs();

        for (String script : SCRIPTS)
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("scripts/" + script), new FileOutputStream(new File(rootRes, script)));

        search.writeScript(rootRes);

        for (String style : STYLESHEETS)
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("stylesheets/" + style), new FileOutputStream(new File(rootRes, style)));
    }

    private void deleteTmpFiles() throws IOException {
        log.info("Deleting temporary directories.");
        Files
            .walk(sourceDir.toPath())
            .filter(f -> f.toFile().isDirectory() && f.getFileName().startsWith(".asciidoctor"))
            .forEach(f -> {
                try {
                    log.info(f);
                    FileUtils.deleteDirectory(f.toFile());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
    }

    /**
     * Extracts ToC to #toc from index file placed on the first place of the root directory.
     * @param html
     * @param target
     */
    private boolean extractToC(String html, Path target) {
        if (!containsIndex(target.toString())) {
            log.debug("Not found index file with ToC");
            return false;
        }

        toc = Jsoup.parse(html, StandardCharsets.UTF_8.name());
        toc = toc.select("body").tagName("div").get(0);
        // remove class="article"
        toc.clearAttributes();
        // add search field
        search.injectField(toc.select("#header"));

        return true;
    }

    private boolean containsIndex(String name) {
        return name.contains("index");
    }

    private String correctHtmlAndCopyResources(Path source, String html, Path target, String pathToRoot, SourceLink linkToSource) throws Exception {
        log.debug("correctHtml targetPath: {}, pathToRoot: {}", target, pathToRoot);

        Document jsoup = Jsoup.parse(html);
        Element head = jsoup.selectFirst("head");
        Element title = head.selectFirst("title");

        // add content to search index
        if (search != null) {
            final String relativePath = targetDir.toPath().relativize(target).toString();
            search.addArticle(new Search.Article(relativePath, title.text(), jsoup.text()));
        }

        // add html title suffix
        String siteTitle = attributes == null ? null : attributes.get(ATTR_SITE_TITLE);
        if (siteTitle != null)
            title.text(title.text() + " | " + siteTitle);

        copyResources(jsoup, source, target);

        injectScriptsAndStyles(head, pathToRoot);

        correctToC(jsoup, target, pathToRoot);

        linkToSource.inject(jsoup, sourceDir.toPath().relativize(source).toString());

        return jsoup.toString();
    }

    private void copyResources(Document jsoup, Path source, Path target) throws IOException {
        for (Link link : Links.getLinks(jsoup)) {
            String href = link.get();

            href = StringUtils.substringBefore(href, "#");
            if (href.endsWith(EXT_HTML))
                continue;

            File resSrc = source.getParent().resolve(href).toFile();
            if (!resSrc.exists() || resSrc.isDirectory()) {
                log.debug("Skipping: {}", resSrc);
                continue;
            }

            String relativePath = DIR_RES + "/" + Paths.get(href).getFileName();
            link.set(relativePath);

            File resTarget = target.getParent().resolve(relativePath).toFile();
            log.info("Copying {} to {}", resSrc, resTarget);
            FileUtils.forceMkdirParent(resTarget);
            if (resTarget.exists() && resTarget.lastModified() == resSrc.lastModified())
                log.info("Not changed.");
            else
                FileUtils.copyFile(resSrc, resTarget);
        }
    }

    private void injectScriptsAndStyles(Element head, String pathToRoot) {
        var pathPrefix = pathToRoot + DIR_RES  + "/";
        for (String script : SCRIPTS_INJECT)
            head.append("<script src='" + pathPrefix + script + "'/>");
        for (String css : STYLESHEETS_INJECT)
            head.append("<link rel='stylesheet' href='" + pathPrefix + css + "'>");
    }

    private void correctToC(Document jsoup, Path target, String pathToRoot) {
        Element pageToC = jsoup.selectFirst("#toc.toc");
        if (pageToC == null)
            return;

        // set class 'toc2' for body to support it
        jsoup.selectFirst("body").addClass("toc2");
        // extract page toc's content
        Element pageToCRootUl = pageToC.selectFirst(".sectlevel1").clone();
        pageToC
            // convert page toc to toc2
            .attr("class", "toc2")
            // replace toc2's content by toc
            .html(toc.toString());

        // going through toc links
        for (Element a : pageToC.select("a")) {
            Link link = new Link(a);
            if (link.isExternalReference())
                continue;

            String href = link.get();

            if (target.endsWith(href)) {
                a.addClass("current");
                // injecting page ToC into the global ToC
                if (pageToCRootUl != null)
                    a.after(pageToCRootUl);
            }
            // correct link URL related to the actual page
            link.set(pathToRoot + href);
            a.attr("title", a.text());
        }

        // add link to root on title
        Element title = pageToC.selectFirst("#header h1");
        if (title != null)
            title.html("<a href='" + pathToRoot + "index.html'>" + title.text() + "</a>");
    }
}
