package org.pzdcdoc.processor.snippet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

public class PossibleLineTest {
    @Test
    public void testMovedSnippet() throws Exception {
       /*  [snippet, from="// h", to="r());", remove-leading="        "]
        link:../main/java/org/pzdcdoc/Generator.java#L80-L85[org.pzdcdoc.Generator] */
        var lines = Files.readAllLines(Path.of("src/main/java/org/pzdcdoc/Generator.java"));

        LineFunction fromF = new LineFunction.Starts("// h");
        LineFunction toF = new LineFunction.Ends("r());");

        final int from = 80; 
        final int to = 85;

        var plFrom = PossibleLine.find(lines, from - 1, null, fromF);
        Assert.assertNotNull(plFrom);
        Assert.assertEquals(from, plFrom.num);

        var plTo = PossibleLine.find(lines, to - 1, plFrom, toF);
        Assert.assertNotNull(plTo);
        Assert.assertEquals(to, plTo.num);

        plTo = PossibleLine.find(lines, to - 2, plFrom, toF);
        Assert.assertNull(plTo);
    }
}
