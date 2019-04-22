package de.pzdc.doc;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.AbstractBlock;
import org.asciidoctor.extension.InlineMacroProcessor;

public class JavaDocLink extends InlineMacroProcessor {
    
    // TODO: Make configurable!
    private final String docUrlPrefix = "http://www.bgcrm.ru/doc/3.0/javadoc/";

    JavaDocLink() {
        super("javadoc");
    }

    @Override
    protected Object process(AbstractBlock parent, String target, Map<String, Object> attributes) {
        Map<String, Object> options = new HashMap<>(2);
        options.put("type", ":link");
        options.put("target", docUrlPrefix + target.replace('.', '/') + ".html");
        return createInline(parent, "anchor", target, attributes, options).convert();
    }

}
