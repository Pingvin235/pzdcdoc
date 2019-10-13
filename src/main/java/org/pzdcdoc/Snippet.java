package org.pzdcdoc;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.Options;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.asciidoctor.jruby.ast.impl.BlockImpl;

@Name("snippet")
//@Contexts({Contexts.LISTING}) 
@ContentModel(ContentModel.SIMPLE)
public class Snippet extends BlockProcessor {

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();
        String yellContent = content.toUpperCase();

        Block block = createBlock(parent, "listing", yellContent, attributes);
        // block.setStyle("source") doesn"t work as expected to enable hightlight: https://github.com/asciidoctor/asciidoctor/blob/master/lib/asciidoctor/converter/html5.rb#L650
        return block;
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