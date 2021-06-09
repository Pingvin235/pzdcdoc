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
import org.pzdcdoc.processor.DrawIO;
import org.pzdcdoc.processor.JavaDocLink;
import org.pzdcdoc.processor.snippet.Snippet;

/**
 * The main runnable class, converting AsciiDoc sources to HTML.
 *
 * @author Shamil Vakhitov
 */
public class Generator {
    private static final Logger log = LogManager.getLogger();

    private static final String DIR_RES = "_res";
    private static final String EXT_ADOC = ".adoc";
    private static final String EXT_HTML = ".html";

    public static final String ATTR_GENERATOR = "generator";
    public static final String ATTR_TARGET = "target";
    public static final String ATTR_SOURCE = "source";

    private final Asciidoctor asciidoctor = Factory.create();

    private static final String[] SCRIPTS = { "jquery-3.3.1.js", "pzdcdoc.js",
        // https://lunrjs.com/guides/language_support.html
        "lunr-2.3.6.js", "lunr.stemmer.support.js", "lunr.multi.js", "lunr.ru.js", "lunr.de.js"
    };
    private static final String[] SCRIPTS_INJECT = ArrayUtils.add(SCRIPTS, Search.SCRIPT);

    private static final String ASCIIDOCTOR_DEFAULT_CSS = "asciidoctor-default.css";
    private static final String PZDCDOC_CSS = "pzdcdoc.css";
    private static final String[] STYLESHEETS = { ASCIIDOCTOR_DEFAULT_CSS, PZDCDOC_CSS, "coderay-asciidoctor.css" };
    private static final String[] STYLESHEETS_INJECT = { PZDCDOC_CSS };

    @Option(required = true, name = "-i", aliases = { "--in" }, usage = "Source directory path")
    private File sourceDir;
    @Option(required = true, name = "-o", aliases = { "--out" }, usage = "Target directory path")
    private File targetDir;

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
        javaExtensionRegistry.block(new Snippet());
        //javaExtensionRegistry.treeprocessor(new Treeprocessor());

        asciidoctor.requireLibrary("asciidoctor-diagram");
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

        FileUtils.deleteDirectory(targetDir);

        process(sourceDir, targetDir, -1, new HashMap<>());
        copyScriptsAndStyles();
        deleteTmpFiles();
        if (errors > 0)
            log.error("PROC ERRORS => " + errors);
        return errors;
    }

    private int check() throws Exception {
        int errors = new Links().checkDir(targetDir);
        if (errors > 0)
            log.error("CHECK ERRORS => " + errors);
        return errors;
    }

    private void process(File source, File target, int depth, Map<String, Object> attributes) throws Exception {
        final String sourceName = source.getName();

        // hidden resources, names started by .
        if (sourceName.startsWith(".")) {
            log.debug("Skip hidden: {}", source);
            return;
        }

        // include - skipping
        if (sourceName.endsWith(".adocf")) {
            log.debug("Skip include: {}", source);
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
                process(file, new File(target.getPath() + "/" + file.getName()), depth + 1, attributes);
        } else {
            if (sourceName.endsWith(EXT_ADOC)) {
                log.info("Processing: " + source);

                Path targetPath = Paths.get(target.getPath().replace(EXT_ADOC, EXT_HTML));

                var attrs = Attributes.builder()
                    .stylesDir(StringUtils.repeat("../", depth) + DIR_RES)
                    .styleSheetName(ASCIIDOCTOR_DEFAULT_CSS)
                    .linkCss(true)
                    .sourceHighlighter("coderay")
                    .icons(Attributes.FONT_ICONS)
                    .tableOfContents(true)
                    .setAnchors(true)
                    .linkAttrs(true)
                    .build();

                attrs.setAttribute("last-update-label", "Powered by <a target='_blank' href='http://pzdcdoc.org'>PzdcDoc</a> at: ");
                attrs.setAttribute(ATTR_SOURCE, source);
                attrs.setAttribute(ATTR_TARGET, targetPath);
                attrs.setAttribute(ATTR_GENERATOR, this);

                attrs.setAttributes(attributes);

                var options = Options.builder()
                    .toFile(false)
                    .headerFooter(true)
                    .safe(SafeMode.UNSAFE)
                    .attributes(attrs)
                    .build();

                String html = asciidoctor.convertFile(source, options);

                html = correctHtmlAndCopyResources(source.toPath(), html, targetPath, depth);

                FileUtils.forceMkdirParent(target);

                FileWriterWithEncoding fos = new FileWriterWithEncoding(targetPath.toFile(), StandardCharsets.UTF_8);
                fos.write(html);
                fos.close();

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

            log.info("Read {} attributes", attributes.size());
        }
        return attributes;
    }

    private void copyScriptsAndStyles() throws IOException {
        log.info("Copy scripts and styles.");

        File rootRes = new File(targetDir + "/" + DIR_RES);
        if (!rootRes.exists()) rootRes.mkdirs();

        for (String script : SCRIPTS)
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("scripts/" + script),
                    new FileOutputStream(rootRes.getAbsolutePath() + "/" + script));

        search.writeScript(rootRes);

        for (String style : STYLESHEETS)
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("stylesheets/" + style),
                    new FileOutputStream(rootRes.getAbsolutePath() + "/" + style));
    }

    private void deleteTmpFiles() throws IOException {
        log.info("Delete temporary directories");
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

    private boolean containsIndex(String name) {
        return name.contains("index");
    }

    private String correctHtmlAndCopyResources(Path source, String html, Path target, int depth) throws Exception {
        log.debug("correctHtml targetPath: {}, deep: {}", target, depth);

        if (toc == null) {
            // the index file must be placed on the top the root directory
            if (containsIndex(target.toString())) {
                toc = Jsoup.parse(html, StandardCharsets.UTF_8.name());
                toc = toc.select("body").tagName("div").get(0);
                // add search field
                search.injectField(toc.select("#header"));
            }
            return html;
        }

        Document jsoup = Jsoup.parse(html);
        Element head = jsoup.selectFirst("head");

        // add content to search index
        if (search != null) {
            final String relativePath = targetDir.toPath().relativize(target).toString().replace('\\', '/');
            search.addArticle(new Search.Article(relativePath, head.select("title").text(), jsoup.text()));
        }

        copyResources(jsoup, source, target);

        injectScriptsAndStyles(depth, head);

        correctToC(jsoup, target, depth);

        html = jsoup.toString();

        return html;
    }

    private void injectScriptsAndStyles(int depth, Element head) {
        var pathPrefix = StringUtils.repeat("../", depth) + DIR_RES  + "/";
        for (String script : SCRIPTS_INJECT)
            head.append("<script src='" + pathPrefix + script + "'/>");
        for (String css : STYLESHEETS_INJECT)
            head.append("<link rel='stylesheet' href='" + pathPrefix + css + "'>");
    }

    private void correctToC(Document jsoup, Path target, int depth) {
        // find of the top ToC
        Element pageToC = jsoup.selectFirst("#toc.toc");
        if (pageToC != null) {
            Element ul = pageToC.selectFirst(".sectlevel1").clone();
            // remove doesn't work properly
            pageToC.html("");
            pageToC = ul;
        }

        // inject left ToC
        jsoup.selectFirst("body").addClass("toc2");
        jsoup.select("#toc").before("<div id=\"toc\" class=\"toc2\">" + toc.toString() + "</div>");

        for (Element a : jsoup.select("#toc.toc2 a")) {
            Link link = new Link(a);
            if (link.isExternalReference())
                continue;

            String href = link.get();

            if (target.endsWith(href)) {
                a.addClass("current");
                if (pageToC != null)
                    a.after(pageToC);
            }
            link.set(StringUtils.repeat("../", depth) + href);
            a.attr("title", a.text());
        }

        // add link to root on title
        Element title = jsoup.selectFirst("#toc #header h1");
        if (title != null) {
            title.html("<a href='" + StringUtils.repeat("../", depth) + "index.html'>" + title.text() + "</a>");
        }
    }

    private void copyResources(Document jsoup, Path source, Path target) throws IOException {
        for (Link link : Links.getLinks(jsoup)) {
            String href = link.get();

            href = StringUtils.substringBefore(href, "#");
            if (href.endsWith(EXT_HTML))
                continue;

            File resSrc = source.getParent().resolve(href).toFile();
            if (!resSrc.exists() || resSrc.isDirectory()) {
                log.debug("Skip: {}", resSrc);
                continue;
            }

            // Ditaa generated images
            if (href.startsWith("diag")) {
                File resTarget = target.getParent().resolve(href).toFile();
                log.info("Move {} to {}", resSrc, resTarget);
                FileUtils.moveFile(resSrc, resTarget);
            } else {
                String relativePath = DIR_RES + "/" + Paths.get(href).getFileName();
                link.set(relativePath);

                File resTarget = target.getParent().resolve(relativePath).toFile();
                log.info("Copy {} to {}", resSrc, resTarget);
                FileUtils.forceMkdirParent(resTarget);
                FileUtils.copyFile(resSrc, resTarget);
            }
        }
    }

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

        if (errors > 0)
            log.error("ERRORS => " + errors);

        System.exit(errors);
    }

}
