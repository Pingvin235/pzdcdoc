package org.pzdcdoc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Backward compatible runner class.
 * 
 * @author Shamil Vakhitov
 */
@Deprecated
public class DocGenerator {
    private static final Logger log = LogManager.getLogger();

    public static void main(String[] args) throws Exception {
        log.warn("Use '{}' main class instead", Generator.class.getName());
        Generator.main(new String[] { "--in", args[1], "--out", args[2] });
        log.warn("Use '{}' main class instead", Generator.class.getName());
    }
}
