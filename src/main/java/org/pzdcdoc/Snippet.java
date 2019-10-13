package org.pzdcdoc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;

@Name("snippet")
@ContentModel(ContentModel.SIMPLE)
public class Snippet extends BlockProcessor {
    private static final Logger log = LogManager.getLogger();

    public static final String ATTRIBUTE = "pzdcdoc-snippet";

    private static final String LINK_PREFIX = "link:";
    private static final Pattern linesRange = Pattern.compile("L(\\d+)(\\-L(\\d+))?");

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();

        attributes = new HashMap<>(attributes);

        try {
            if (content.startsWith(LINK_PREFIX)) {
                String path = content.substring(LINK_PREFIX.length());
                String fragment = StringUtils.substringAfter(path, "#");
                if (StringUtils.isNotBlank(fragment))
                    path = path.substring(0, path.length() - fragment.length() - 1);
    
                File source = (File) parent.getDocument().getAttribute(DocGenerator.SOURCE_ATTR);
                if (source == null)
                    throw new Exception("Not found source file attribute.");
                
                File snippet = source.toPath().getParent().resolve(path).toFile();
                if (!snippet.exists())
                    throw new Exception("File doesn't exist: " + snippet);

                attributes.put("language", "java");
                attributes.put(ATTRIBUTE, "");
            }
        } catch (Exception e) {
            log.error("Not found source file attribute.");
        }

        return createBlock(parent, "listing", content, attributes);
    }
    
}