package org.codelibs.elasticsearch.sstmpl.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.ScriptTemplateException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.PagedBytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.CompiledScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptService.ScriptType;

public class SearchActionFilter extends AbstractComponent implements
        ActionFilter {

    private int order;

    private ScriptService scriptService;

    @Inject
    public SearchActionFilter(Settings settings,
            final ScriptService scriptService) {
        super(settings);
        this.scriptService = scriptService;

        order = settings.getAsInt("indices.sstmpl.filter.order", 1);
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public void apply(String action, ActionRequest request,
            ActionListener listener, ActionFilterChain chain) {
        if (!SearchAction.INSTANCE.name().equals(action)) {
            chain.proceed(action, request, listener);
            return;
        }

        SearchRequest searchRequest = (SearchRequest) request;
        BytesReference templateSource = searchRequest.templateSource();
        if (templateSource != null) {
            try {
                XContentParser parser = XContentFactory
                        .xContent(templateSource).createParser(templateSource);
                Map<String, Object> sourceMap = parser.mapAndClose();
                Object langObj = sourceMap.get("lang");
                if (langObj != null) {
                    searchRequest = createScriptSearchRequest(searchRequest,
                            langObj.toString(), sourceMap);
                }
            } catch (Exception e) {
                listener.onFailure(e);
                return;
            }
        }

        chain.proceed(action, searchRequest, listener);
    }

    private SearchRequest createScriptSearchRequest(
            SearchRequest searchRequest, String lang,
            Map<String, Object> sourceMap) {
        Map<String, Object> paramMap = (Map<String, Object>) sourceMap
                .get("params");

        String script;
        ScriptType scriptType;
        Object templateObj = sourceMap.get("template");
        if (templateObj instanceof Map) {
            Map<String, Object> templateMap = (Map<String, Object>) templateObj;

            String templateName = (String) templateMap.get("id");
            String templateFile = (String) templateMap.get("file");
            if (templateName != null) {
                // id
                scriptType = ScriptType.INDEXED;
                script = templateName;
            } else if (templateFile != null) {
                // file
                scriptType = ScriptType.FILE;
                script = templateFile;
            } else {
                // query/filtered
                scriptType = ScriptType.INLINE;
                try {
                    script = XContentFactory.jsonBuilder()
                            .value((Map<String, Object>) templateObj).string();
                } catch (IOException e) {
                    throw new ScriptTemplateException(
                            "Failed to parse template object: " + templateObj);
                }
            }
        } else if (templateObj instanceof String) {
            // query/filtered
            scriptType = ScriptType.INLINE;
            script = templateObj.toString();
        } else {
            throw new ScriptTemplateException("template is not an object.");
        }

        final CompiledScript compiledScript = scriptService.compile(lang,
                script, scriptType);
        ExecutableScript executable = scriptService.executable(compiledScript,
                paramMap != null ? paramMap : Collections.emptyMap());
        Object result = executable.run();
        if (result == null) {
            throw new ScriptTemplateException("Query DSL is null.");
        }

        searchRequest.templateName(null);
        searchRequest.templateSource(null, false);
        searchRequest.templateType(null);
        searchRequest.source(getResultAsString(result));

        return searchRequest;
    }

    private String getResultAsString(Object result) {
        if (result instanceof String) {
            return result.toString();
        } else if (result instanceof PagedBytesReference) {
            return ((BytesReference) result).toUtf8();
        } else {
            throw new ScriptTemplateException(
                    "The result of script-based search template is " + result
                            + ".");
        }
    }

    @Override
    public void apply(String action, ActionResponse response,
            ActionListener listener, ActionFilterChain chain) {
        chain.proceed(action, response, listener);
    }

}
