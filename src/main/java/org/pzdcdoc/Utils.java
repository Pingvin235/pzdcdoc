package org.pzdcdoc;

/**
 * Utilities.
 * 
 * @author Shamil Vakhitov
 */
public class Utils {
    /**
     * Replaces path separators to right slashes.
     * @param path original path.
     * @return replaced path.
     */
    public static final String pathToUnix(String path) {
        return path.replace('\\', '/');
    }
}
