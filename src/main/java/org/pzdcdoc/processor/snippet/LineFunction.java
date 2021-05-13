package org.pzdcdoc.processor.snippet;

import java.util.function.Function;

/**
 * Line checking functions.
 * 
 * @author Shamil Vakhitov
 */
abstract class LineFunction implements Function<String, Boolean> {
    static final class Starts extends LineFunction {
        private final String prefix;

        Starts(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Boolean apply(String line) {
            return line.trim().startsWith(prefix);
        }
    }

    static final class Ends extends LineFunction {
        private final String suffix;

        Ends(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public Boolean apply(String line) {
            return line.trim().endsWith(suffix);
        }
    }
}
