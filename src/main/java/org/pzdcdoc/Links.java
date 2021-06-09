package org.pzdcdoc;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Links {
    private static final Logger log = LogManager.getLogger();
    
    private int errors;
    
    int checkDir(File outputDir) throws Exception {
        log.info("Start checking");
        errors = 0;
        checkFile(outputDir);
        return errors;
    }
    
    private void checkFile(File file) throws Exception {
        if (file.isDirectory()) {
            for (File child : file.listFiles())
                checkFile(child);
            return;
        }

        if (!file.getName().endsWith(".html"))
            return;

        log.info("Checking file: " + file);

        org.jsoup.nodes.Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());
        for (Link link : getLinks(doc)) {
            String href = link.get();

            log.debug("Checking: {}", href);
            
            String fragment = null;
            int pos = href.indexOf('#');
            if (pos >= 0) {
                fragment = href.substring(pos + 1);
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
            
            if (!StringUtils.isBlank(fragment) &&
                !IOUtils.toString(new FileInputStream(refFile), StandardCharsets.UTF_8.name()).contains("id=\"" + fragment + "\"")) {
                log.error("Not found referenced fragment: " + fragment);
                errors++;
            }
        }
    }

    /**
     * Extracts 'href' attributes from 'a' tags and 'src' from 'img'.
     * @param doc
     * @return
     */
    public static Iterable<Link> getLinks(Document doc) {
        List<Link> result = new ArrayList<>();

        for (Element ref : doc.select("a")) {
            Link link = new Link(ref);
            if (!link.isExternalReference())
                result.add(link);
        }

        for (Element img : doc.select("img")) {
            Link link = new Link(img);
            if (!link.isExternalReference())
                result.add(link);
        }

        return result;
    }
    
}
