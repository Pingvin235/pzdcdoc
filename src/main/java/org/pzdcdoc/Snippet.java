package org.pzdcdoc;

import java.util.Map;

import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;

@Name("snippet")
//@Contexts({ Contexts.PARAGRAPH })
@ContentModel(ContentModel.SIMPLE)
public class Snippet extends BlockProcessor {

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();
        String yellContent = content.toUpperCase();

        return createBlock(parent, "paragraph", yellContent, attributes);
    }

    /*@Override
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
    }*/

}