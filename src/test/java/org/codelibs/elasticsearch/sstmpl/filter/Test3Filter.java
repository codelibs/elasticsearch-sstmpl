package org.codelibs.elasticsearch.sstmpl.filter;

import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.chain.SearchTemplateChain;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.script.ScriptService.ScriptType;

public class Test3Filter implements SearchTemplateFilter {

    @Override
    public int order() {
        return 3;
    }

    @Override
    public String doCreate(final String lang, final String script,
            final ScriptType scriptType, final Map<String, Object> paramMap,
            final SearchTemplateChain chain) {
        if (!paramMap.containsKey("filter1")) {
            throw new IllegalStateException("filter1 is null.");
        }
        if (!paramMap.containsKey("filter2")) {
            throw new IllegalStateException("filter2 is null.");
        }
        final MapBuilder<String, Object> builder = MapBuilder.newMapBuilder(
                paramMap).put("filter3", Boolean.TRUE);
        if (paramMap.containsKey("my_fieldx")) {
            builder.put("my_field", paramMap.get("my_fieldx"));
        }
        if (paramMap.containsKey("my_valuex")) {
            builder.put("my_value", paramMap.get("my_valuex"));
        }
        if (paramMap.containsKey("my_sizex")) {
            builder.put("my_size", paramMap.get("my_sizex"));
        }
        final Map<String, Object> newParamMap = builder.map();
        return chain.doCreate(lang, script, scriptType, newParamMap);
    }

}
