package org.pzdcdoc.processor;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;
import org.pzdcdoc.Generator;

/**
 * AsciiDoctor-J processor converting 'javadoc:package.Class[]' to JavaDoc URLs.
 *
 * @author Shamil Vakhitov
 */
@Name("javadoc")
public class JavaDocLink extends InlineMacroProcessor {
    /** JavaDoc URL path prefix, can be absolute or related to the target root directory */
    private static final String ATTR_PATH_PREFIX = "pzdc-javadoc";

    @Override
    public PhraseNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        String prefix = (String) parent.getDocument().getAttribute(ATTR_PATH_PREFIX);
        if (prefix == null)
            throw new UnsupportedOperationException("No proper configuration defined for javadoc macros");

        prefix = prefix.trim();
        if (!prefix.endsWith("/"))
            prefix = prefix + "/";

        // the map must be modifiable
        Map<String, Object> options = new HashMap<>();
        options.put("type", ":link");

        String path = target.replace('.', '/') + ".html";

        String targetOption = null;
        // absolute path prefix
        if (prefix.contains("://"))
            targetOption = prefix + path;
        // relative path prefix
        else {
            String pathToRoot = (String) parent.getDocument().getAttribute(Generator.ATTR_PATH_TO_ROOT);
            targetOption = pathToRoot + prefix + path;
        }

        options.put("target", targetOption);

        return createPhraseNode(parent, "anchor", target, attributes, options);
    }
}
