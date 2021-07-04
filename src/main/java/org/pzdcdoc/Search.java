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
import org.jsoup.select.Elements;

/**
 * Embedded JS search.
 * 
 * @author Shamil Vakhitov
 */
public class Search {
    private static final Logger log = LogManager.getLogger();

    static final String SCRIPT = "pzsearch.js";

    private List<Article> articles = new ArrayList<>();
    
    /**
     * Add an article to search index.
     * @param article
     */
    public void addArticle(Article article) {
        articles.add(article);
    }

    /**
     * Injects search HTML field to document.
     * @param header target HTML element.
     */
    public void injectField(Elements header) {
        header.after("<div id='search'><div id='search-input'><input type='text' placeholder='Search'/></div><div id='search-count'></div></div>");
    }

    /**
     * Generates search JS file.
     * @param rootRes
     */
    public void writeScript(File rootRes) {
        log.info("Write search script.");
        try (Writer out = new OutputStreamWriter(new FileOutputStream(rootRes.getAbsolutePath() + "/" + SCRIPT), StandardCharsets.UTF_8)) {
            out.write("$(function () {");
            out.write("$$.documents = ");
            out.write(new ObjectMapper().writeValueAsString(articles));
            out.write(";$$.initSearch();});");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Article - search item.
     */
    public static class Article {
        private final String ref;
        private final String title;
        private final String content;

        public Article(String ref, String title, String content) {
            this.ref = Utils.pathToUnix(ref);
            this.title = title;
            this.content = content.toLowerCase();
            log.debug("Add article, ref: {}, title: {}", ref, title);
        }

        /**
         * Relative URL.
         * @return
         */
        public String getRef() {
            return ref;
        }

        /**
         * Title.
         * @return
         */
        public String getTitle() {
            return title;
        }

        /**
         * Plain text content.
         * @return
         */
        public String getContent() {
            return content;
        }
    }
}