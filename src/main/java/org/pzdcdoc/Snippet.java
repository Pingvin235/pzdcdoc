package org.pzdcdoc;

import java.io.File;
import java.nio.file.Files;
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

    // marker about generating the block by snippet macros
    public static final String ATTR_MARKER = "pzdcdoc-snippet";
    public static final String ATTR_FROM = "from";
    public static final String ATTR_TO = "to";

    private static final String LINK_PREFIX = "link:";
    private static final Pattern linesRange = Pattern.compile("L(\\d+)(\\-L(\\d+))?");

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String content = reader.read();

        attributes = new HashMap<>(attributes);

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

                StringBuilder contentBuilder = new StringBuilder((lineTo - lineFrom) * 100);
                contentBuilder.append("// PzdcDoc snippet of: '");
                contentBuilder.append(path);
                contentBuilder.append("', lines: ");
                contentBuilder.append(lineFrom);
                contentBuilder.append(" - ");
                contentBuilder.append(lineTo);
                contentBuilder.append("\n");

                for (int lineNum = lineFrom; lineNum <= lineTo; lineNum++) {
                    String line = lines.get(lineNum - 1);
                    contentBuilder.append(line);
                    contentBuilder.append("\n");
                }

                content = contentBuilder.toString();

                attributes.put("language", lang); 
                attributes.put(ATTR_MARKER, "");
            }
        } catch (Exception e) {
            log.error("Not found source file attribute.");
        }

        return createBlock(parent, "listing", content, attributes);
    }
    
}