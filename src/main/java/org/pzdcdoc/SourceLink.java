package org.pzdcdoc;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;

/**
 * Link to source injector.
 * 
 * @author Shamil Vakhitov
 */
public class SourceLink {
    private static final String ATTR_SOURCE_LINK_ROOT = "pzdc-source-link-root";
    private final String linkRootUrl;

    public SourceLink(Map<String, Object> attributes) {
        this.linkRootUrl = (String) attributes.get(ATTR_SOURCE_LINK_ROOT);
    }

    /**
     * Inserts HTTP link to a source file.
     * @param jsoup document.
     * @param sourceRelativePath path string, relative to a source dir.
     */
    public void inject(Document jsoup, String sourceRelativePath) {
        if (StringUtils.isBlank(linkRootUrl))
            return;
        var el = jsoup.selectFirst("#header > h1");
        if (el != null)
            el.append("<a title='View source' href='" + linkRootUrl + "/" + Utils.pathToUnix(sourceRelativePath) + "' id='source-link'>S</a>");
    }
}
