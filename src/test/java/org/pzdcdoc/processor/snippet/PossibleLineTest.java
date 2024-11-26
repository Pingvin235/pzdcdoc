package org.pzdcdoc.processor.snippet;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for possible line predictor.
 *
 * @author Shamil Vakhitov
 */
public class PossibleLineTest {
    @Test
    public void testMovedSnippet() throws Exception {
        var lines = Files.readAllLines(Path.of("src/main/java/org/pzdcdoc/Generator.java"));

        LineFunction fromF = new LineFunction.Starts("// h");
        LineFunction toF = new LineFunction.Ends("r());");

        // take the right values from snippet in demo.adoc
<<<<<<< HEAD
        final int from = 85;
        final int to = 90;
=======
        final int from = 122;
        final int to = 128;
>>>>>>> 142ff45 (Java Doc fixes)

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
