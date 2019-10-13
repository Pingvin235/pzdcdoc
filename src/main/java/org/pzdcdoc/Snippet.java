package org.pzdcdoc;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;

@Name("snippet")
@ContentModel(ContentModel.SIMPLE)
public class Snippet extends BlockProcessor {

    public static final String ATTRIBUTE = "pzdcdoc-snippet";

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();

        attributes = new HashMap<>(attributes);
        attributes.put("language", "java");
        attributes.put(ATTRIBUTE, "");

        return createBlock(parent, "listing", content, attributes);
    }
    
}