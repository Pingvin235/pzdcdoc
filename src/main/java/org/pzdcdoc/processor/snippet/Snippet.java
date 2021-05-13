package org.pzdcdoc.processor.snippet;

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
import org.pzdcdoc.Generator;

/**
 * AsciiDoctor-J processor supporting 'live snippets'.
 * 
 * @author Shamil Vakhitov
 */
@Name("snippet")
@ContentModel(ContentModel.SIMPLE)
public class Snippet extends BlockProcessor {
    private static final Logger log = LogManager.getLogger();

    public static final String ATTR_FROM = "from";
    public static final String ATTR_TO = "to";
    public static final String ATTR_REMOVE_LEADING = "remove-leading";

    private static final String LINK_PREFIX = "link:";
    private static final Pattern LINES_RANGE = Pattern.compile("L(\\d+)(\\-L(\\d+))?");

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();

        Generator generator = (Generator) parent.getDocument().getAttribute(Generator.ATTR_GENERATOR);

        List<String> contentList = new ArrayList<>(100);
        try {
            if (content.startsWith(LINK_PREFIX)) {
                String path = content.substring(LINK_PREFIX.length());

                String title = StringUtils.substringBetween(path, "[", "]");
                path = StringUtils.substringBeforeLast(path, "[");

                String fragment = StringUtils.substringAfter(path, "#");
                if (StringUtils.isNotBlank(fragment))
                    path = path.substring(0, path.length() - fragment.length() - 1);

                File source = (File) parent.getDocument().getAttribute(Generator.ATTR_SOURCE);
                if (source == null)
                    throw new Exception("Not found source file attribute.");

                File snippet = source.toPath().getParent().resolve(path).toFile();
                if (!snippet.exists())
                    throw new Exception("File doesn't exist: " + snippet);

                List<String> lines = Files.readAllLines(snippet.toPath());
                int lineFrom = 1;
                int lineTo = lines.size();

                if (StringUtils.isNotBlank(fragment)) {
                    Matcher m = LINES_RANGE.matcher(fragment);
                    if (m.find()) {
                        int line = NumberUtils.toInt(m.group(1));
                        if (lineFrom <= line && line <= lineTo)
                            lineFrom = line;
                        line = NumberUtils.toInt(m.group(3));
                        if (lineFrom <= line && line <= lineTo)
                            lineTo = line;
                    }
                }

                // TODO: Make mapping extension - lang
                String lang = StringUtils.substringAfterLast(path, ".");

                addComment(path, contentList, title, lineFrom, lineTo, lang);

                include(generator, attributes, path, contentList, lines, lineFrom, lineTo);

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

    /**
     * Adds generated commented line at beginning of snippet.
     * @param path snippet's file path.
     * @param contentList snippet's lines to be included.
     * @param title title path, shown as link.
     * @param lineFrom start line of the snippet.
     * @param lineTo end line of the snippet.
     * @param lang snippets programming language.
     */
    private void addComment(String path, List<String> contentList, String title, int lineFrom, int lineTo, String lang) {
        contentList.add(LangGroup.of(lang)
                .commment("PzdcDoc snippet of: '" + (StringUtils.isNotBlank(title) ? title : path) + "', lines: " + lineFrom + " - " + lineTo));
        contentList.add("");
    }

    /**
     * Check and include a snippet's lines.
     * @param generator generator.
     * @param attributes Asciidoctor-J attributes.
     * @param path snippet's file path.
     * @param contentList snippet's lines to be included.
     * @param lines snippet's file lines.
     * @param lineFrom line from, 1 based.
     * @param lineTo line end, 1 based.
     */
    private void include(Generator generator, Map<String, Object> attributes, String path, List<String> contentList, List<String> lines,
            int lineFrom, int lineTo) {
        String from = (String) attributes.get(ATTR_FROM);
        String to = (String) attributes.get(ATTR_TO);
        String removeLeading = (String) attributes.get(ATTR_REMOVE_LEADING);

        LineFunction fromF = from != null ? new LineFunction.Starts(from) : LineFunction.PASS;
        LineFunction toF = to != null ? new LineFunction.Ends(to) : LineFunction.PASS;

        PossibleLine plFrom = null;

        for (int lineNum = lineFrom; lineNum <= lineTo; lineNum++) {
            String line = lines.get(lineNum - 1);

            if (lineNum == lineFrom && !fromF.apply(line)) {
                plFrom = PossibleLine.find(lines, lineNum, null, fromF);
                log.error("Snippet '{}' doesn't start from: '{}', line number: {}{}, content: {}", path, from, String.valueOf(lineNum),
                        PossibleLine.toString(plFrom), line.trim());
                generator.error();
            }

            if (lineNum == lineTo && !toF.apply(line)) {
                var plTo = PossibleLine.find(lines, lineNum, plFrom, toF);
                log.error("Snippet '{}' doesn't end on: '{}', line number: {}{}, content: {}", path, to, String.valueOf(lineNum),
                        PossibleLine.toString(plTo), line.trim());
                generator.error();
            }

            if (removeLeading != null && line.startsWith(removeLeading))
                line = line.substring(removeLeading.length());

            contentList.add(line);
        }
    }

}