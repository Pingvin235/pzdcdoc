package org.pzdcdoc;

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

// https://github.com/asciidoctor/asciidoctorj/blob/v2.1.0/docs/integrator-guide.adoc#treeprocessor
public class Treeprocessor extends org.asciidoctor.extension.Treeprocessor {

    @Override
    public Document process(Document document) {
        processBlock((StructuralNode) document);
        return document;
    }

    private void processBlock(StructuralNode block) {
        if (block.hasAttribute(Snippet.ATTRIBUTE))
            block.setStyle("source");

        for (StructuralNode child : block.getBlocks())
            processBlock((StructuralNode) child);
    }
}