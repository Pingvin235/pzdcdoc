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
    
    private static final String RES = "_res";
    
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
    // global attributes from configuration file
    private Map<String, Object> attributes;
    // 
    private Search search = new Search();
    
    public DocGenerator(String configDir, String sourceDir, String targetDir) throws Exception {
        this.configDir = new File(configDir);
        this.sourceDir = new File(sourceDir);
        this.targetDir = new File(targetDir);
        
        JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
        javaExtensionRegistry.inlineMacro(new JavaDocLink());
        
        FileUtils.deleteDirectory(new File(targetDir));
    }
    
    public void process() throws Exception {
        process(sourceDir, targetDir, -1, false);
        copyScriptsAndStyles();
    }

    public int check() throws Exception {
        int errors = new LinksChecker(targetDir).check();
        if (errors > 0)
            log.error("ERROR COUNT => " + errors);
        return errors;
    }

    public void process(File source, File target, int depth, boolean resource) throws Exception {
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
            final boolean resourceDir = resource || RES.equals(sourceName);
            
            File[] files = source.listFiles();
            
            // index.adoc in the root folder must be processed first to be included in all files after
            Arrays.sort(files, (f1, f2) -> { 
                if (containsIndex(f1.getName()))
                    return -1;
                if (containsIndex(f2.getName()))
                    return 1;
                return 0;
            });
            
            for (File file : files)
                process(file, new File(target.getPath() + "/" + file.getName()), depth + 1, resourceDir);
        } else {
            if (sourceName.endsWith(".adoc")) {
                log.info("Processing: " + source);

                if (containsIndex(sourceName) && attributes == null)
                    loadAttributes(source);

                Attributes attrs = AttributesBuilder.attributes()
                        .stylesDir(StringUtils.repeat("../", depth) + RES)
                        .linkCss(true)
                        .sourceHighlighter("coderay")
                        .icons(Attributes.FONT_ICONS)
                        .tableOfContents(true)
                        .setAnchors(true)
                        .get();
                
                attrs.setAttribute("last-update-label", "Powered by <a target='_blank' href='http://pzdcdoc.org'>PzdcDoc</a> at: ");

                if (attributes != null)
                    attrs.setAttributes(attributes);

                Options options = OptionsBuilder.options()
                        .toFile(false)
                        .headerFooter(true)
                        .safe(SafeMode.UNSAFE)
                        .attributes(attrs)
                        .get();

                String html = asciidoctor.convertFile(source, options);

                String targetPath = target.getPath().replace(".adoc", ".html").replace('\\','/');

                html = correctHtml(html, targetPath, depth);
                
                FileUtils.forceMkdirParent(target);
               
                FileWriterWithEncoding fos = new FileWriterWithEncoding(targetPath, StandardCharsets.UTF_8);
                fos.write(html);
                fos.close();
                
                return;
            } 
            // copy resource
            else if (resource)
                FileUtils.copyFile(source, target);
        }
    }

    private void loadAttributes(File source) throws DocumentException {
        Path configuration = source.toPath().getParent().resolve("pzdcdoc.xml");
        if (configuration.toFile().exists()) {
            log.info("Processing configuration: {}", configuration);
            org.dom4j.Document document = new SAXReader().read(configuration.toFile());
            
            attributes = new HashMap<>();

            for (Node attr : document.selectNodes("//attributes/*"))
                attributes.put(attr.getName(), attr.getText());

            log.info("Read {} attributes", attributes.size());
        }
    }
        
    public void copyScriptsAndStyles() throws IOException {
        log.info("Copy scripts and styles.");
        
        File rootRes = new File(targetDir + "/" + RES);
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
    
    private String correctHtml(String html, String targetPath, int depth) throws Exception {
        log.debug("correctHtml targetPath: {}, deep: {}", targetPath, depth);
        
        if (toc == null) {
            // the index file must be placed on the top the root directory
            if (containsIndex(targetPath)) {
                toc = Jsoup.parse(html, StandardCharsets.UTF_8.name());
                toc = toc.select("body").tagName("div").get(0);
                // add search field
                toc.select("#header").after("<div id='search'><input type='text' placeholder='Search'/></div>");
            }
            return html;
        }
        
        Document jsoup = Jsoup.parse(html);
        Element head = jsoup.selectFirst("head");

        if (search != null) {
            final String relativePath = targetDir.toPath().relativize(Paths.get(targetPath)).toString();
            search.addArticle(new Search.Article(relativePath, head.select("title").text(), jsoup.text()));
        }
        
        // inject JS files
        for (String script : SCRIPTS_INJECT)
            head.append("<script src='" + StringUtils.repeat("../", depth) + RES  + "/" + script + "'/>");
        
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

            if (targetPath.endsWith(href)) {
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
    
    public static void main(String[] args) throws Exception {
        // TODO: Use args4j.
        String configDir = args[0], sourceDir = args[1], targetDir = args[2];
        
        DocGenerator gen = new DocGenerator(configDir, sourceDir, targetDir);
        gen.process();
        int errors = gen.check();
        
        log.info("DONE!");
        
        System.exit(errors);
    }

}
