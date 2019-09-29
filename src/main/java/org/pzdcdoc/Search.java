package org.pzdcdoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Search {
    private static final Logger log = LogManager.getLogger();

    static final String SCRIPT = "pzsearch.js";

    private List<Article> articles = new ArrayList<>();
    
    public void addArticle(Article article) {
        articles.add(article);
    }

    public void writeScript(File rootRes) {
        addArticle(new Article("Тест", "Тест", "Тест foo"));

        log.info("Write search script.");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(rootRes.getAbsolutePath() + "/" + SCRIPT), StandardCharsets.UTF_8)) {
            out.write("$$.documents = ");
            out.write(new ObjectMapper().writeValueAsString(articles));
            out.write(";");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static class Article {
        private final String ref;
        private final String title;
        private final String content;

        public Article(String ref, String title, String content) {
            this.ref = ref;
            this.title = title;
            this.content = content;
            log.debug("Add article, ref: {}, title: {}", ref, title);
        }

        public String getRef() {
            return ref;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }
}