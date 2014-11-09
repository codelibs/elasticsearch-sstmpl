package org.codelibs.elasticsearch.sstmpl;

public class ScriptTemplateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ScriptTemplateException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ScriptTemplateException(final String message) {
        super(message);
    }

}
