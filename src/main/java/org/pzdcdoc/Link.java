package org.pzdcdoc;

import org.jsoup.nodes.Element;

/**
 * HTML reference object: a, img.
 *
 * @author Shamil Vakhitov
 */
class Link {
    private final Element node;
    private final String attrName;

    /**
     * Constructor
     * @param node html element
     */
    Link(Element node) {
        this.node = node;
        this.attrName = "a".equals(node.nodeName()) ? "href" : "src";
    }

    /**
     * Link reference from node specific attribute: href or src
     * @return link value
     */
    public String get() {
        return node.attr(attrName);
    }

    /**
     * Set reference to a node specific attribute
     * @param value link value
     */
    public void set(String value) {
        node.attr(attrName, value);
    }

    /**
     * Is the reference points to an external resource: URL, mail
     * @return is the reference external or not
     */
    public boolean isExternalReference() {
        String href = get();
        return
            href.startsWith("#_") ||
            href.startsWith("mailto:") ||
            // TODO: Check also.
            href.contains("://");
    }
}