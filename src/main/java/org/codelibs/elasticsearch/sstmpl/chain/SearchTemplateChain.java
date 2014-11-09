package org.codelibs.elasticsearch.sstmpl.chain;

import java.util.Collections;
import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.ScriptTemplateException;
import org.codelibs.elasticsearch.sstmpl.filter.SearchTemplateFilter;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptService.ScriptType;

public class SearchTemplateChain {

    private ScriptService scriptService;

    private SearchTemplateFilter[] filters;

    int position = 0;

    public SearchTemplateChain(final ScriptService scriptService,
            final SearchTemplateFilter[] filters) {
        this.scriptService = scriptService;
        this.filters = filters;
    }

    public String doCreate(final String lang, final String script,
            final ScriptType scriptType, final Map<String, Object> paramMap) {
        if (position < filters.length) {
            final SearchTemplateFilter filter = filters[position];
            position++;
            return filter.doCreate(lang, script, scriptType, paramMap, this);
        } else {
            final CompiledScript compiledScript = scriptService.compile(lang,
                    script, scriptType);
            final ExecutableScript executable = scriptService.executable(
                    compiledScript,
                    paramMap != null ? paramMap : Collections.emptyMap());
            final Object result = executable.run();
            if (result instanceof String) {
                return (String) result;
            } else if (result instanceof BytesReference) {
                return ((BytesReference) result).toUtf8();
            } else {
                throw new ScriptTemplateException("Query DSL is null.");
            }
        }
    }

}
