package org.pzdcdoc.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;
import org.pzdcdoc.Generator;

import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * AsciiDoctor-J processor converting DrawIO diagrams to images.
 *
 * @author Shamil Vakhitov
 */
@Name("drawio")
public class DrawIO extends InlineMacroProcessor {
    private static final Logger log = LogManager.getLogger();

    /** Attribute defining JavaDoc root URL. */
    private static final String ATTR_CONVERTER = "pzdc-drawio-converter";
    private static final String ATTR_TIMEOUT = "pzdc-drawio-request-timeout-sec";

    // private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // private OkHttpClient http;

    @Override
    public PhraseNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        Map<String, Object> options = new HashMap<>();
        options.put("type", "image");
        options.put("target", convert(parent.getDocument(), target));

        // Image attributes may be found: https://docs.asciidoctor.org/asciidoctorj/latest/extensions/inline-macro-processor/
        return createPhraseNode(parent, "image", (String) null, attributes, options);
    }

    /**
     * Processes Draw.IO diagram.
     * @param doc AsciiDoctor document.
     * @param target document replated path to '.drawio' XML source file.
     * @return path to converted image.
     */
    private String convert(Document doc, String target) {
        try {
            String converterUrl = (String) doc.getAttribute(ATTR_CONVERTER);
            if (StringUtils.isBlank(converterUrl)) {
                throw new IllegalArgumentException("Attribute '" + ATTR_CONVERTER + "' is not defined");
            }

            int timeout = NumberUtils.toInt((String) doc.getAttribute(ATTR_TIMEOUT), 60);

            String srcDocDir = (String) doc.getAttribute("docdir");
            String srcPath = srcDocDir + "/" + target;

            // TODO: Think about supporting different formats except SVG.
            final var format = "svg";

            target = StringUtils.substringBeforeLast(target, ".") + "." + format;

            Path targetDocPath = (Path) doc.getAttribute(Generator.ATTR_TARGET);
            Path targetPath = targetDocPath.getParent().resolve(target);

            convert(converterUrl, timeout, srcPath, targetPath, format);

            return target;
        } catch (Exception e) {
            log.error("Export error", e);
            return target;
        }
    }

    /**
     * Sends request to converter container:
     * https://hub.docker.com/r/tomkludy/drawio-renderer
     *
     * @param converterUrl URL to convert
     * @param timeout wait timeout in seconds.
     * @param srcPath path of source Draw.IO file.
     * @param targetPath path of resulting file.
     * @param format format, currently only 'svg' is supported.
     * @throws Exception
    */
    private void convert(String converterUrl, int timeout, String srcPath, Path targetPath, String format) throws Exception {
        log.info("Converting URL: {}, srcPath: {}, targetPath: {}", converterUrl, srcPath, targetPath);

        var targetFile = targetPath.toFile();
        if (targetFile.exists() && new File(srcPath).lastModified() < targetFile.lastModified()) {
            log.info("Skipping converting. Target file already exists and newer than source.");
            return;
        }

        long time = System.currentTimeMillis();

        String json = MAPPER.writeValueAsString(Map.of(
            "source", IOUtils.toString(new FileInputStream(srcPath), StandardCharsets.UTF_8),
            "format", format
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(converterUrl))
            .timeout(Duration.ofSeconds(timeout))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = HttpClient.newBuilder()
            .build()
            .send(request, BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new IOException("HTTP response code: " + response.statusCode() + "; body: " + response.body());

        targetPath.getParent().toFile().mkdirs();
        IOUtils.write(response.body(), new FileOutputStream(targetPath.toString()), StandardCharsets.UTF_8);

        log.info("Time => {} ms.", System.currentTimeMillis() - time);
    }
}
