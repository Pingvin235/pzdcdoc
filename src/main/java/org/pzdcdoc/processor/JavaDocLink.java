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
    /** JavaDoc root URL attribute name */
    private static final String ATTR_ROOT_URL = "pzdc-javadoc-root-url";
    /** JavaDoc root relative path */
    private static final String ATTR_ROOT_PATH = "pzdc-javadoc-root-path";

    @Override
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        String urlPrefix = (String) parent.getDocument().getAttribute(ATTR_ROOT_URL);
        if (urlPrefix != null) {
            // the map must be modifiable
            Map<String, Object> options = new HashMap<>();
            options.put("type", ":link");
            options.put("target", urlPrefix + target.replace('.', '/') + ".html");
            return createPhraseNode(parent, "anchor", target, attributes, options);
        }

        throw new UnsupportedOperationException("No proper configuration defined for javadoc macros");
    }

}
