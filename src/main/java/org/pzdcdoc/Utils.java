package org.pzdcdoc;

/**
 * Utilites.
 * 
 * @author Shamil Vakhitov
 */
public class Utils {
    /**
     * Replaces path separators to right slashes.
     * @param path
     * @return
     */
    public static final String pathToUnix(String path) {
        return path.replace('\\', '/');
    }
}
