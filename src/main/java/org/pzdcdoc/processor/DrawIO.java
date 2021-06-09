package org.pzdcdoc.processor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;
import org.pzdcdoc.Generator;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

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
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient.Builder().readTimeout(20, TimeUnit.SECONDS).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        Map<String, Object> options = new HashMap<>();
        options.put("type", "image");
        options.put("target", export(parent.getDocument(), target));

        // Image attributes may be found: https://docs.asciidoctor.org/asciidoctorj/latest/extensions/inline-macro-processor/
        return createPhraseNode(parent, "image", (String) null, attributes, options);
    }

    /**
     * Processes Draw.IO diagram.
     * @param doc AsciiDoctor document.
     * @param target document replated path to '.drawio' XML source file.
     * @return path to converted image.
     */
    private String export(Document doc, String target) {
        try {
            String converterUrl = (String) doc.getAttribute(ATTR_CONVERTER);
            if (StringUtils.isBlank(converterUrl)) {
                throw new IllegalArgumentException("Attribute '" + ATTR_CONVERTER + "' is not defined");
            }

            String srcDocDir = (String) doc.getAttribute("docdir");
            String srcPath = srcDocDir + "/" + target;

            // TODO: Think about supporting different formats except SVG.
            final var format = "svg";

            target = StringUtils.substringBeforeLast(target, ".") + "." + format;

            Path targetDocPath = (Path) doc.getAttribute(Generator.ATTR_TARGET);
            Path targetPath = targetDocPath.getParent().resolve(target);

            // TODO: Conditional running,  
            convert(converterUrl, srcPath, targetPath, format);
            
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
     * @param srcPath path of source Draw.IO file.
     * @param targetPath path of resulting file.
     * @param format format, currently only 'svg' is supported.
     * @throws JsonProcessingException
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void convert(String converterUrl, String srcPath, Path targetPath, String format)
            throws JsonProcessingException, IOException, FileNotFoundException {
        log.info("Converting URL: {}, srcPath: {}, targetPath: {}", converterUrl, srcPath, targetPath);
        long time = System.currentTimeMillis();

        var json = mapper.writeValueAsString(Map.of(
            "source", IOUtils.toString(new FileInputStream(srcPath), StandardCharsets.UTF_8),
            "format", format
        ));

        var request = new Request.Builder()
            .url(converterUrl)
            .post(RequestBody.create(JSON, json))
            .build();

        try (var response = client.newCall(request).execute()) {
            targetPath.getParent().toFile().mkdirs();
            IOUtils.write(response.body().string(), new FileOutputStream(targetPath.toString()), StandardCharsets.UTF_8);
        }

        log.info("Time => {} ms.", System.currentTimeMillis() - time);
    }

}
