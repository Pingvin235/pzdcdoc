package de.pzdc.doc;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class LinksChecker {
    private static final Logger log = LogManager.getLogger();
    
    private final File outputDir;
    private int errors;
    
    LinksChecker(File outputDir) {
        this.outputDir = outputDir;
    }
    
    int check() throws Exception {
        log.info("Start checking");
        check(outputDir);
        return errors;
    }
    
    private void check(File file) throws Exception {
        if (file.isDirectory()) {
            for (File child : file.listFiles())
                check(child);
            return;
        }

        if (!file.getName().endsWith(".html"))
            return;

        log.info("Checking file: " + file);

        org.jsoup.nodes.Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());
        for (Element ref : doc.select("a")) {
            String href = ref.attr("href");
            String anchor = null;
            
            if (href.startsWith("#_") || href.startsWith("mailto:"))
                continue;
            
            // TODO: Check also.
            if (href.contains("://"))
                continue;
            
            int pos = href.indexOf('#');
            if (pos >= 0) {
                anchor = href.substring(pos + 1);
                href = href.substring(0, pos);
            }
            
            File refFile = file; 
            if (!StringUtils.isBlank(href)) {
                refFile = file.toPath().getParent().resolve(href).toFile();
                if (!refFile.exists()) {
                    log.error("Not found referenced file: " + href);
                    errors++;
                    continue;
                }
            }
            
            if (!StringUtils.isBlank(anchor) &&
                !IOUtils.toString(new FileInputStream(refFile), StandardCharsets.UTF_8.name()).contains("id=\"" + anchor + "\"")) {
                log.error("Not found referenced anchor: " + anchor);
                errors++;
            }
        }
        
        for (Element img : doc.select("img")) {
            String src = img.attr("src");
            // TODO: Check also.
            if (src.contains("://"))
                continue;
            
            File refFile = file.toPath().getParent().resolve(src).toFile();
            if (!refFile.exists()) {
                log.error("Not found referenced image: " + src);
                errors++;
                continue;
            }            
        }
    }
        
}
