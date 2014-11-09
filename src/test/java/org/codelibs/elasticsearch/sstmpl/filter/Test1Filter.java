package org.codelibs.elasticsearch.sstmpl.filter;

import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.chain.SearchTemplateChain;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.script.ScriptService.ScriptType;

public class Test1Filter implements SearchTemplateFilter {

    @Override
    public int order() {
        return 1;
    }

    @Override
    public String doCreate(final String lang, final String script,
            final ScriptType scriptType, final Map<String, Object> paramMap,
            final SearchTemplateChain chain) {
        if (paramMap.containsKey("filter2")) {
            throw new IllegalStateException("filter2 is not null.");
        }
        if (paramMap.containsKey("filter3")) {
            throw new IllegalStateException("filter3 is not null.");
        }
        final Map<String, Object> newParamMap = MapBuilder
                .newMapBuilder(paramMap).put("filter1", Boolean.TRUE).map();
        return chain.doCreate(lang, script, scriptType, newParamMap);
    }

}
