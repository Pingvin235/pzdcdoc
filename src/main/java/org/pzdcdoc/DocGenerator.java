package org.pzdcdoc;

/**
 * Backward compatible runner class.
 * 
 * @author Shamil Vakhitov
 */
@Deprecated
public class DocGenerator {
    public static void main(String[] args) throws Exception {
        Generator.main(new String[] { "--in", args[1], "--out", args[2] });
    }
}
