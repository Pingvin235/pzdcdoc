package org.pzdcdoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
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
    private final File outputDir;
    
    private final String[] scripts = new String[] {"jquery-3.3.1.js", "pzdcdoc.js"};
    private final String[] stylesheets = new String[] {"asciidoctor.css", "coderay-asciidoctor.css"};
    
    private Element toc;
    
    public DocGenerator(String configDir, String sourceDir, String outputDir) throws Exception {
        this.configDir = new File(configDir);
        this.sourceDir = new File(sourceDir);
        this.outputDir = new File(outputDir);
        
        JavaExtensionRegistry javaExtensionRegistry = asciidoctor.javaExtensionRegistry();
        javaExtensionRegistry.inlineMacro(new JavaDocLink());
        
        FileUtils.deleteDirectory(new File(outputDir));
    }
    
    public void process() throws Exception {
        copyScriptsAndStyles();
        process(sourceDir, outputDir, -1, false);
    }

    public int check() throws Exception {
        int errors = new LinksChecker(outputDir).check();
        if (errors > 0)
            log.error("ERROR COUNT => " + errors);
        return errors;
    }

    public void process(File source, File target, int deep, boolean resource) throws Exception {
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

        final Predicate<String> isIndex = (name) -> name.contains("index");
        
        if (source.isDirectory()) {
            final boolean resourceDir = resource || RES.equals(sourceName);
            
            File[] files = source.listFiles();
            
            // index.adoc in the root folder must be processed first to be included in all files after
            Arrays.sort(files, (f1, f2) -> { 
                if (isIndex.test(f1.getName()))
                    return -1;
                if (isIndex.test(f2.getName()))
                    return 1;
                return 0;
            });
            
            for (File file : files)
                process(file, new File(target.getPath() + "/" + file.getName()), deep + 1, resourceDir);
        } else {
            if (sourceName.endsWith(".adoc")) {
                log.info("Processing: " + source);

                if (isIndex.test(sourceName)) {
                    Path configuration = source.toPath().getParent().resolve("pzdcdoc.xml");
                    if (configuration.toFile().exists()) {
                        log.info("Processing configuration: {}", configuration);
                        org.dom4j.Document document = new SAXReader().read(configuration.toFile());
                        
                        Map<String, String> attributes = new HashMap<>();

                        for (Node attr : document.selectNodes("//attributes/*"))
                            attributes.put(attr.getName(), attr.getText());

                        System.out.println(attributes);
                    }
                }

                // TODO: Move all the attributes to configuration.
                Attributes attrs = AttributesBuilder.attributes()
                        .stylesDir(StringUtils.repeat("../", deep) + RES)
                        .linkCss(true)
                        .sourceHighlighter("coderay")
                        .icons(Attributes.FONT_ICONS)
                        .tableOfContents(true)
                        .setAnchors(true)
                        .get();
                
                attrs.setAttribute("last-update-label", "Generated by <a target='_blank' href='http://pzdcdoc.org'>PzdcDoc</a>, last change: ");

                

               
                /* // TODO: Make configurable.
                // https://asciidoctor.org/docs/user-manual/#customizing-labels
                // https://github.com/asciidoctor/asciidoctor/blob/master/lib/asciidoctor/document.rb#L255
                attrs.setAttribute("appendix-caption", "Приложение");
                attrs.setAttribute("caution-caption", "Внимание");
                attrs.setAttribute("example-caption", "Пример");
                attrs.setAttribute("figure-caption", "Рисунок");
                attrs.setAttribute("important-caption", "Важно");
                attrs.setAttribute("last-update-label", "Сгенерировано <a target='_blank' href='http://pzdcdoc.org'>PzdcDoc</a> Последнее изменение: ");
                attrs.setAttribute("manname-title", "НАЗВАНИЕ");
                attrs.setAttribute("note-caption", "Примечание");
                attrs.setAttribute("table-caption", "Таблица");
                attrs.setAttribute("tip-caption", "Подсказка");
                attrs.setAttribute("toc-title", "Содержание");
                attrs.setAttribute("untitled-label", "Без названия");
                attrs.setAttribute("version-label", "Версия");
                attrs.setAttribute("warning-caption", "Предупреждение");*/

                Options options = OptionsBuilder.options()
                        .toFile(false)
                        .headerFooter(true)
                        .safe(SafeMode.UNSAFE)
                        .attributes(attrs)
                        .get();

                HashMap<String, Object> options2 = new HashMap<>();
                org.asciidoctor.ast.Document doc = asciidoctor.load(IOUtils.toString(new FileInputStream(source), StandardCharsets.UTF_8), new HashMap<>());
                
                String html = asciidoctor.convertFile(source, options);
                
                String targetPath = target.getPath().replace(".adoc", ".html").replace('\\','/');
                
                html = correctHtml(html, targetPath, deep);
                
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
        
    public void copyScriptsAndStyles() throws IOException {
        log.info("Copy scripts and styles.");
        
        File rootRes = new File(outputDir + "/" + RES);
        if (!rootRes.exists()) rootRes.mkdirs();
        
        for (String script : scripts)
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("scripts/" + script),
                    new FileOutputStream(rootRes.getAbsolutePath() + "/" + script));
        
        for (String style : stylesheets)
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream("stylesheets/" + style),
                    new FileOutputStream(rootRes.getAbsolutePath() + "/" + style));
    }
    
    private String correctHtml(String html, String targetPath, int deep) throws Exception {
        log.debug("correctHtml targetPath: {}, deep: {}", targetPath, deep);
        
        if (toc == null) {
            // The index file must be placed on the top directory.
            if (targetPath.contains("index")) {
                toc = Jsoup.parse(html, StandardCharsets.UTF_8.name());
                toc = toc.select("body").tagName("div").get(0);
            }
            return html;
        }
        
        Document jsoup = Jsoup.parse(html);
        
        // inject JS files
        Element head = jsoup.selectFirst("head");
        for (String script : scripts)
            head.append("<script src='" + StringUtils.repeat("../", deep) + RES  + "/" + script + "'/>");
        
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
            a.attr("href", StringUtils.repeat("../", deep) + href);
            a.attr("title", a.text());
        }
        
        html = jsoup.toString();
        
        return html;    
    }
    
    public static void main(String[] args) throws Exception {
        // TODO: Use args4j.
        String configDir = args[0], sourceDir = args[1], outputDir = args[2];
        
        DocGenerator gen = new DocGenerator(configDir, sourceDir, outputDir);
        gen.process();
        int errors = gen.check();
        
        log.info("DONE!");
        
        System.exit(errors);
    }

}
