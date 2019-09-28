package org.pzdcdoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Search {
    private static final Logger log = LogManager.getLogger();

    static final String SCRIPT = "pzsearch.js";

    
    public void addArticle(String content) {

    }

    public void writeScript(File rootRes) {
        log.info("Write search script.");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(rootRes.getAbsolutePath() + "/" + SCRIPT), StandardCharsets.UTF_8)) {
            out.write("$$.documents = []");
        } catch (Exception e) {
            log.error(e);
        }
    }
}