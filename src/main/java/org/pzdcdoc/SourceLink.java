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
    private final String linkRootUrl;

    /** Constructor */
    SourceLink(Map<String, Object> attributes) {
        this.linkRootUrl = (String) attributes.get("pzdc-source-link-root");
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
