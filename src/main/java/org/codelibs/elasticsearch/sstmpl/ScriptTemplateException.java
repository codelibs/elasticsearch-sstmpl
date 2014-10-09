package org.codelibs.elasticsearch.sstmpl;

public class ScriptTemplateException extends RuntimeException {

    public ScriptTemplateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScriptTemplateException(String message) {
        super(message);
    }

}
