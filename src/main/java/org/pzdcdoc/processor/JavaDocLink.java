package org.pzdcdoc.processor;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;

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
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        String urlPrefix = (String) parent.getDocument().getAttribute(ATTR_PATH_PREFIX);
        if (urlPrefix == null)
            throw new UnsupportedOperationException("No proper configuration defined for javadoc macros");

        // the map must be modifiable
        Map<String, Object> options = new HashMap<>();
        options.put("type", ":link");

        String path = target.replace('.', '/') + ".html";

        String targetOption = null;
        // absolute path prefix
        if (urlPrefix.contains("://"))
            targetOption = urlPrefix + path;
        // relative
        else {

        }

        options.put("target", targetOption);

        return createPhraseNode(parent, "anchor", target, attributes, options);
    }
}
