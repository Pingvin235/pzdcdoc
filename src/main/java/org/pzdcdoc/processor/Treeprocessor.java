package org.pzdcdoc.processor;

import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;

// Used only for debugging.
// https://github.com/asciidoctor/asciidoctorj/blob/v2.1.0/docs/integrator-guide.adoc#treeprocessor
public class Treeprocessor extends org.asciidoctor.extension.Treeprocessor {

    @Override
    public Document process(Document document) {
        processBlock((StructuralNode) document);
        return document;
    }

    /* Useful debugging method, for checking through generated tree. */
    private void processBlock(StructuralNode block) {
        for (StructuralNode child : block.getBlocks())
            processBlock((StructuralNode) child);
    }
}