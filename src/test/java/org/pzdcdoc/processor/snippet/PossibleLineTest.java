package org.pzdcdoc.processor.snippet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class PossibleLineTest {
    @Test
    public void testMovedSnippet() throws Exception {
       /*  [snippet, from="// h", to="r());", remove-leading="        "]
        link:../main/java/org/pzdcdoc/DocGenerator.java#L81-L86[org.pzdcdoc.DocGenerator] */
        var lines = Files.readAllLines(Path.of("src/main/java/org/pzdcdoc/DocGenerator.java"));
        
    }
}
