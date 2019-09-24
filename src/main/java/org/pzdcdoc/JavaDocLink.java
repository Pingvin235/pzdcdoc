package org.pzdcdoc;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class JavaDocLink extends InlineMacroProcessor {
    
    // TODO: Make configurable!
    private final String docUrlPrefix = "http://www.bgcrm.ru/doc/3.0/javadoc/";

    JavaDocLink() {
        super("javadoc");
    }

    @Override
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        Map<String, Object> options = new HashMap<>(2);
        options.put("type", ":link");
        options.put("target", docUrlPrefix + target.replace('.', '/') + ".html");
        return createPhraseNode(parent, "anchor", target, attributes, options).convert();
    }

}
