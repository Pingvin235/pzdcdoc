package org.pzdcdoc.processor.drawio;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockMacroProcessor;
import org.asciidoctor.extension.Name;

/**
 * AsciiDoctor-J processor converting DrawIO diagrams to BLOCK images.
 *
 * @author Shamil Vakhitov
 */
@Name("drawio")
public class DrawIOBlock extends BlockMacroProcessor {

    @Override
    public StructuralNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        String path = DrawIO.convert(parent.getDocument(), target);

        StringBuilder img = new StringBuilder(100).append("<img src=\"").append(path).append("\"");
        attr(img, attributes, "width");
        attr(img, attributes, "alt");
        img.append("/>");

        String content = new StringBuilder(300)
            .append("<div class=\"imageblock\">")
                .append("<div class=\"content\">")
                    .append(img)
                .append("</div>")
            .append("</div>")
        .toString();

        return createBlock(parent, "pass", content);
    }

    private void attr(StringBuilder img, Map<String, Object> attributes, String name) {
        String width = (String) attributes.get(name);
        if (StringUtils.isNotBlank(width))
            img.append(" ").append(name).append("=\"").append(width).append("\"");
    }

}
