package org.pzdcdoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class DocGenerator {
    private static final Logger log = LogManager.getLogger();

    private static final String DIR_RES = "_res";
    private static final String EXT_ADOC = ".adoc";
    private static final String EXT_HTML = ".html";
    
    private final Asciidoctor asciidoctor = Factory.create();
    
    @SuppressWarnings("unused")
    private final File configDir;
    private final File sourceDir;
    private final File targetDir;
    
    private static final String[] SCRIPTS = new String[] {"jquery-3.3.1.js", 
        // https://lunrjs.com/guides/language_support.html
        "lunr-2.3.6.js", "lunr.stemmer.support.js", "lunr.multi.js", "lunr.ru.js", "lunr.de.js",
        "pzdcdoc.js"};
    
    private static final String[] SCRIPTS_INJECT = ArrayUtils.add(SCRIPTS, Search.SCRIPT);
    private static final String[] STYLESHEETS = new String[] {"asciidoctor.css", "coderay-asciidoctor.css"};

    // cached ToC from index.adoc for injecting everywhere
    private Element toc;
    private Search search = new Search();
    
    public DocGenerator(String configDir, String sourceDir, String targetDir) throws Exception {
        this.configDir = new File(configDir);
        this.sourceDir = new File(sourceDir);
        this.targetDir = new File(targetDir);
        
        // https://github.com/asciidoctor/asciidoctorj/blob/v2.1.0/docs/integrator-guide.adoc
        JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
        javaExtensionRegistry.inlineMacro(new JavaDocLink());
        javaExtensionRegistry.blockMacro(new Snippet());

        asciidoctor.requireLibrary("asciidoctor-diagram");
        
        FileUtils.deleteDirectory(new File(targetDir));
    }
    
    public void process() throws Exception {
        process(sourceDir, targetDir, -1, new HashMap<>());
        copyScriptsAndStyles();
    }

    public int check() throws Exception {
        int errors = new Links().checkDir(targetDir);
        if (errors > 0)
            log.error("ERROR COUNT => " + errors);
        return errors;
    }

    public void process(File source, File target, int depth, Map<String, Object> attributes) throws Exception {
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

                Attributes attrs = AttributesBuilder.attributes()
                        .stylesDir(StringUtils.repeat("../", depth) + DIR_RES)
                        .linkCss(true)
                        .sourceHighlighter("coderay")
                        .icons(Attributes.FONT_ICONS)
                        .tableOfContents(true)
                        .setAnchors(true)
                        .linkAttrs(true)
                        .get();
                
                attrs.setAttribute("last-update-label", "Powered by <a target='_blank' href='http://pzdcdoc.org'>PzdcDoc</a> at: ");

                attrs.setAttributes(attributes);

                Options options = OptionsBuilder.options()
                        .toFile(false)
                        .headerFooter(true)
                        .safe(SafeMode.UNSAFE)
                        .attributes(attrs)
                        .get();

                String html = asciidoctor.convertFile(source, options);

                Path targetPath = Paths.get(target.getPath().replace(EXT_ADOC, EXT_HTML));

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
        
    public void copyScriptsAndStyles() throws IOException {
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
        
        // inject JS files
        for (String script : SCRIPTS_INJECT)
            head.append("<script src='" + StringUtils.repeat("../", depth) + DIR_RES  + "/" + script + "'/>");
        
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
            String href = a.attr("href");
            if (Links.isExternalReference(href))
                continue;

            if (target.endsWith(href)) {
                a.addClass("current");
                if (pageToC != null)
                    a.after(pageToC);
            }
            a.attr("href", StringUtils.repeat("../", depth) + href);
            a.attr("title", a.text());
        }
        
        html = jsoup.toString();
        
        return html;
    }

    private void copyResources(Document jsoup, Path source, Path target) throws IOException {
        for (String href : new Links().getLinks(jsoup)) {
            href = StringUtils.substringBefore(href, "#");
            if (href.endsWith(EXT_HTML))
                continue;

            File resSrc = source.getParent().resolve(href).toFile();
            if (!resSrc.exists() || resSrc.isDirectory()) {
                log.debug("Skip: {}", resSrc);
                continue;
            }
            File resTarget = target.getParent().resolve(href).toFile();

            FileUtils.forceMkdirParent(resTarget);

            if (href.startsWith("diag")) {
                log.info("Move {} to {}", resSrc, resTarget);
                FileUtils.moveFile(resSrc, resTarget);
            } else {
                log.info("Copy {} to {}", resSrc, resTarget);
                FileUtils.copyFile(resSrc, resTarget);
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        String configDir = args[0], sourceDir = args[1], targetDir = args[2];
        
        DocGenerator gen = new DocGenerator(configDir, sourceDir, targetDir);
        gen.process();
        int errors = gen.check();
        
        log.info("DONE!");
        
        System.exit(errors);
    }

}
