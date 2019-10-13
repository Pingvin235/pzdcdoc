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

    private static final String LINK_PREFIX = "link:";
    private static final Pattern linesRange = Pattern.compile("L(\\d+)(\\-L(\\d+))?");

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();

        attributes = new HashMap<>();

        List<String> contentList = new ArrayList<>();

        try {
            if (content.startsWith(LINK_PREFIX)) {
                String path = content.substring(LINK_PREFIX.length());
                String fragment = StringUtils.substringAfter(path, "#");
                if (StringUtils.isNotBlank(fragment))
                    path = path.substring(0, path.length() - fragment.length() - 1);
    
                File source = (File) parent.getDocument().getAttribute(DocGenerator.SOURCE_ATTR);
                if (source == null)
                    throw new Exception("Not found source file attribute.");
                
                File snippet = source.toPath().getParent().resolve(path).toFile();
                if (!snippet.exists())
                    throw new Exception("File doesn't exist: " + snippet);

                // TODO: Make mapping extension - lang
                String lang = StringUtils.substringAfterLast(path, ".");
                attributes.put("language", lang);

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

                contentList.add("// PzdcDoc snippet of: '" + path + "', lines: " + lineFrom + " - " + lineTo);

                for (int lineNum = lineFrom; lineNum <= lineTo; lineNum++)
                    contentList.add(lines.get(lineNum - 1));
            }
        } catch (Exception e) {
            log.error("Not found source file attribute.");
        }

        attributes.put("style", "source");

        return createBlock(parent, "listing", contentList, attributes);
    }
    
}