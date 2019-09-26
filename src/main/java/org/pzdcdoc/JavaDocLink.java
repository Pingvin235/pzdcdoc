package org.pzdcdoc;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;

@Name("javadoc")
public class JavaDocLink extends InlineMacroProcessor {

    private static final String ATTR_PATH_PREFIX_NAME = "pzdc-javadoc";

    @Override
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        Map<String, Object> options = new HashMap<>(2);
        options.put("type", ":link");
        options.put("target", (String) parent.getDocument().getAttr(ATTR_PATH_PREFIX_NAME) + target.replace('.', '/') + ".html");
        return createPhraseNode(parent, "anchor", target, attributes, options);
    }

}
