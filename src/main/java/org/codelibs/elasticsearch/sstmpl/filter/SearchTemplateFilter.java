package org.codelibs.elasticsearch.sstmpl.filter;

import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.chain.SearchTemplateChain;
import org.elasticsearch.script.ScriptType;

public interface SearchTemplateFilter {

    /**
     * The position of the filter in the chain. Execution is done from lowest order to highest.
     */
    int order();

    String doCreate(String lang, String script, ScriptType scriptType,
            Map<String, Object> paramMap, SearchTemplateChain chain);
}
