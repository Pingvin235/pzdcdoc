package org.pzdcdoc;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;

@Name("snippet")
@ContentModel(ContentModel.SIMPLE)
public class Snippet extends BlockProcessor {
    private static final Logger log = LogManager.getLogger();

    public static final String ATTR_FROM = "from";
    public static final String ATTR_TO = "to";
    public static final String ATTR_REMOVE_LEADING = "remove-leading";

    private static final String LINK_PREFIX = "link:";
    private static final Pattern linesRange = Pattern.compile("L(\\d+)(\\-L(\\d+))?");

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();

        DocGenerator generator = (DocGenerator) parent.getDocument().getAttribute(DocGenerator.ATTR_GENERATOR);

        List<String> contentList = new ArrayList<>();
        try {
            if (content.startsWith(LINK_PREFIX)) {
                String path = content.substring(LINK_PREFIX.length());

                String title = StringUtils.substringBetween(path, "[", "]");
                path = StringUtils.substringBeforeLast(path, "[");

                String fragment = StringUtils.substringAfter(path, "#");
                if (StringUtils.isNotBlank(fragment))
                    path = path.substring(0, path.length() - fragment.length() - 1);

                File source = (File) parent.getDocument().getAttribute(DocGenerator.ATTR_SOURCE);
                if (source == null)
                    throw new Exception("Not found source file attribute.");
                
                File snippet = source.toPath().getParent().resolve(path).toFile();
                if (!snippet.exists()) 
                    throw new Exception("File doesn't exist: " + snippet);

                List<String> lines = Files.readAllLines(snippet.toPath());
                int lineFrom = 1;
                int lineTo = lines.size();

                if (StringUtils.isNotBlank(fragment)) {
                    Matcher m = linesRange.matcher(fragment);
                    if (m.find()) {
                        int line  = NumberUtils.toInt(m.group(1));
                        if (lineFrom <= line && line <= lineTo)
                            lineFrom = line;
                        line =  NumberUtils.toInt(m.group(3));
                        if (lineFrom <= line && line <= lineTo)
                            lineTo = line;
                    }
                }

                // TODO: Make mapping extension - lang
                String lang = StringUtils.substringAfterLast(path, ".");

                addComment(contentList, path, title, lineFrom, lineTo, lang);

                String from = (String) attributes.get(ATTR_FROM);
                String to = (String) attributes.get(ATTR_TO);
                String removeLeading = (String) attributes.get(ATTR_REMOVE_LEADING);

                for (int lineNum = lineFrom; lineNum <= lineTo; lineNum++) {
                    String line = lines.get(lineNum - 1);

                    if (from != null && lineNum == lineFrom && !line.trim().startsWith(from)) {
                        log.error("Snippet '{}' doesn't start from: '{}', line: {}", path, from, line.trim());
                        generator.error();
                    }
                    if (to != null && lineNum == lineTo && !line.trim().endsWith(to)) {
                        log.error("Snippet '{}' doesn't end on: '{}', line: {}", path, to, line.trim());
                        generator.error();
                    }
                    
                    if (removeLeading != null && line.startsWith(removeLeading))
                        line = line.substring(removeLeading.length());

                    contentList.add(line);
                }

                attributes = new HashMap<>();
                attributes.put("style", "source");
                if (StringUtils.isNotBlank(lang))
                    attributes.put("language", lang);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            generator.error();
        }

        return createBlock(parent, "listing", contentList, attributes);
    }

    private static enum LangGroup {
        C {
            @Override
            protected String commment(String line) {
                return "// " + line;
            }
        },
        XML {
            @Override
            protected String commment(String line) {
                return "<!-- " + line + " -->";
            }
        },
        SH {
            @Override
            protected String commment(String line) {
                return "# " + line;
            }
        },
        JSP {
            @Override
            protected String commment(String line) {
                return "<%-- " + line + " --%>";
            }
        };

        protected abstract String commment(String line);

        private static LangGroup of(String lang) {
            switch(lang) {
                case "xml":
                case "html":
                case "htm":
                    return XML;
                case "pl":
                case "py":
                case "sh":
                    return SH;
                case "jsp":
                    return JSP;
                default:
                    return C;
            }
        }
    }

    private void addComment(List<String> contentList, String path, String title, int lineFrom, int lineTo, String lang) {
        contentList.add(LangGroup.of(lang).commment("PzdcDoc snippet of: '" + (StringUtils.isNotBlank(title) ? title : path) + "', lines: " + lineFrom + " - " + lineTo));
        contentList.add("");
    }

}