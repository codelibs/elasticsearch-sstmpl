package org.codelibs.elasticsearch.sstmpl.filter;

import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.chain.SearchTemplateChain;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.script.ScriptType;

public class Test2Filter implements SearchTemplateFilter {

    @Override
    public int order() {
        return 2;
    }

    @Override
    public String doCreate(final String lang, final String script,
            final ScriptType scriptType, final Map<String, Object> paramMap,
            final SearchTemplateChain chain) {
        if (!paramMap.containsKey("filter1")) {
            throw new IllegalStateException("filter1 is null.");
        }
        if (paramMap.containsKey("filter3")) {
            throw new IllegalStateException("filter3 is not null.");
        }
        final Map<String, Object> newParamMap = MapBuilder
                .newMapBuilder(paramMap).put("filter2", Boolean.TRUE).map();
        return chain.doCreate(lang, script, scriptType, newParamMap);
    }

}
