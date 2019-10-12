package org.pzdcdoc;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;

@Name("link")
public class Link extends InlineMacroProcessor {

    @Override
    public Object process(ContentNode parent, String target, Map<String, Object> attributes) {
        Map<String, Object> options = new HashMap<>(2);
        options.put("type", ":link");
        options.put("target", target);
        
        String from = (String)attributes.get("from");
        String to = (String)attributes.get("to");

        if (StringUtils.isNotBlank(from) || StringUtils.isNotBlank(to)) {
            System.out.println();
        }
        
        return createPhraseNode(parent, "anchor", target, attributes, options);
    }   

}