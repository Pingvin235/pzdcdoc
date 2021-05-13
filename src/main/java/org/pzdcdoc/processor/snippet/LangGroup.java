package org.pzdcdoc.processor.snippet;

/**
 * Programming language group.
 * 
 * @author Shamil Vakhitov
 */
enum LangGroup {
    C {
        @Override
        protected String commment(String line) {
            return "// " + line;
        }
    },
    XML {
        @Override
        protected String commment(String line) {
            return "<!-- " + line + " -->";
        }
    },
    SH {
        @Override
        protected String commment(String line) {
            return "# " + line;
        }
    },
    JSP {
        @Override
        protected String commment(String line) {
            return "<%-- " + line + " --%>";
        }
    };

    protected abstract String commment(String line);

    static LangGroup of(String lang) {
        switch(lang) {
            case "xml":
            case "html":
            case "htm":
                return XML;
            case "pl":
            case "py":
            case "sh":
                return SH;
            case "jsp":
                return JSP;
            default:
                return C;
        }
    }
}