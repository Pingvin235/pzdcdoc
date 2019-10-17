package org.pzdcdoc;

import org.jsoup.nodes.Element;

public class Link {
    private final Element node;
    private final String attrName;

    public Link(Element node) {
        this.node = node;
        this.attrName = "a".equals(node.nodeName()) ? "href" : "src";
    }

    public String get() {
        return node.attr(attrName);
    }

    public void set(String value) {
        node.attr(attrName, value);
    }

    public boolean isExternalReference() {
        String href = get();
        return 
            href.startsWith("#_") || 
            href.startsWith("mailto:") ||
            // TODO: Check also.
            href.contains("://");
    }
}