package org.pzdcdoc;

import java.nio.file.Path;
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

    public void inject(Document jsoup, Path source) {
        if (StringUtils.isBlank(linkRootUrl))
            return;
        var el = jsoup.selectFirst("#header > h1");
        if (el != null)
            el.append("<a title='View source' href='" + linkRootUrl + "/" + source.toString().replace('\\', '/') + "' id='source-link'>S</a>");
    }
}
