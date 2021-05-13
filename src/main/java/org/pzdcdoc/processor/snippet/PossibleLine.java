package org.pzdcdoc.processor.snippet;

import java.util.List;
import java.util.function.Function;

/**
 * Possible line of a code snippet for case of 
 * line inserts or removals before the snippet.
 * 
 * @author Shamil Vakhitov
 */
class PossibleLine {
    /** How many lines before and after to search. */ 
    private static final int SEARCH_OFFSET = 30;

    private final int num;
    private final int offset;

    private PossibleLine(int line, int shift) {
        this.num = line;
        this.offset = shift;
    }

    static String toString(PossibleLine line) {
        return line == null ? "" : " (possible: " + line.num + ")";
    }

    static PossibleLine find(List<String> lines, int num, PossibleLine before, Function<String, Boolean> checkF) {
        if (before != null) {
            num = num + before.offset;
            if (0 < num && num <= lines.size() -1 && checkF.apply(lines.get(num - 1)))
                return new PossibleLine(num, before.offset);
            return null;
        }

        for (int offset = 1; offset <= SEARCH_OFFSET; offset++) {
            int numBefore = num - offset;
            if (1 < numBefore && checkF.apply(lines.get(numBefore - 1)))
                return new PossibleLine(numBefore, -offset);
            
            int numAfter = num + offset;
            if (numAfter <= lines.size() && checkF.apply(lines.get(numAfter - 1)))
                return new PossibleLine(numAfter, offset);
        }

        return null;
    }
}