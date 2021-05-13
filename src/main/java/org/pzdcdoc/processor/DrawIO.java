package org.pzdcdoc.processor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;
import org.pzdcdoc.DocGenerator;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * AsciiDoctor-J processor converting DrawIO diagrams to images.
 * For converting is used:
 * https://hub.docker.com/r/tomkludy/drawio-renderer
 *
 * @author Shamil Vakhitov
 */
@Name("drawio")
public class DrawIO extends InlineMacroProcessor {
    private static final Logger log = LogManager.getLogger();

    /** Attribute defining JavaDoc root URL. */
    private static final String ATTR_CONVERTER = "pzdc-drawio-converter";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        Map<String, Object> options = new HashMap<>();
        options.put("type", "image");
        options.put("target", export(parent.getDocument(), target));

        // Image attributes may be found: https://docs.asciidoctor.org/asciidoctorj/latest/extensions/inline-macro-processor/
        return createPhraseNode(parent, "image", (String) null, attributes, options);
    }

    private String export(Document doc, String target) {
        try {
            String converterUrl = (String) doc.getAttribute(ATTR_CONVERTER);
            if (StringUtils.isBlank(converterUrl)) {
                throw new IllegalArgumentException("Attribute '" + ATTR_CONVERTER + "' is not defined");
            }

            String srcDocDir = (String) doc.getAttribute("docdir");
            String srcPath = srcDocDir + "/" + target;

            // TODO: Support different formats except SVG.
            target = StringUtils.substringBeforeLast(target, ".") + ".svg";

            Path targetDocPath = (Path) doc.getAttribute(DocGenerator.ATTR_TARGET);
            Path targetPath = targetDocPath.getParent().resolve(target);

            log.info("Converting URL: {}, srcPath: {}, targetPath: {}", converterUrl, srcPath, targetPath);
            long time = System.currentTimeMillis();

            var data = Map.of(
                "source", IOUtils.toString(new FileInputStream(srcPath), StandardCharsets.UTF_8),
                "format", "svg"
            );

            String json = mapper.writeValueAsString(data);
            var body = RequestBody.create(JSON, json);
            
            var request = new Request.Builder()
                .url(converterUrl)
                .post(body)
                .build();
            try (var response = client.newCall(request).execute()) {
                targetPath.getParent().toFile().mkdirs();
                IOUtils.write(response.body().string(), new FileOutputStream(targetPath.toString()), StandardCharsets.UTF_8);
            }

            log.info("Time => {} ms.", System.currentTimeMillis() - time);
            
            return target;
        } catch (Exception e) {
            log.error("Export error", e);
            return target;
        }
    }

}
