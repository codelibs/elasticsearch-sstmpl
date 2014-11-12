package org.codelibs.elasticsearch.sstmpl.filter;

import java.io.IOException;
import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.ScriptTemplateException;
import org.codelibs.elasticsearch.sstmpl.chain.SearchTemplateChain;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptService.ScriptType;

public class SearchActionFilter extends AbstractComponent implements
        ActionFilter {

    private int order;

    private ScriptService scriptService;

    private SearchTemplateFilters filters;

    private ThreadLocal<SearchType> currentSearchType = new ThreadLocal<>();

    @Inject
    public SearchActionFilter(final Settings settings,
            final ScriptService scriptService,
            final SearchTemplateFilters filters) {
        super(settings);
        this.scriptService = scriptService;
        this.filters = filters;

        order = settings.getAsInt("indices.sstmpl.filter.order", 1);
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public void apply(final String action,
            @SuppressWarnings("rawtypes") final ActionRequest request,
            @SuppressWarnings("rawtypes") final ActionListener listener,
            final ActionFilterChain chain) {
        if (!SearchAction.INSTANCE.name().equals(action)) {
            chain.proceed(action, request, listener);
            return;
        }

        SearchRequest searchRequest = (SearchRequest) request;
        final SearchType searchType = currentSearchType.get();
        if (searchType == null) {
            try {
                currentSearchType.set(searchRequest.searchType());
                chain.proceed(action, request, listener);
            } finally {
                currentSearchType.remove();
            }
        } else {
            final BytesReference templateSource = searchRequest
                    .templateSource();
            if (templateSource != null) {
                try {
                    final XContentParser parser = XContentFactory.xContent(
                            templateSource).createParser(templateSource);
                    final Map<String, Object> sourceMap = parser.mapAndClose();
                    final Object langObj = sourceMap.get("lang");
                    if (langObj != null) {
                        searchRequest = createScriptSearchRequest(
                                searchRequest, langObj.toString(), sourceMap);
                    }
                } catch (final Exception e) {
                    listener.onFailure(e);
                    return;
                }
            }

            chain.proceed(action, searchRequest, listener);
        }
    }

    private SearchRequest createScriptSearchRequest(
            final SearchRequest searchRequest, final String lang,
            final Map<String, Object> sourceMap) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> paramMap = (Map<String, Object>) sourceMap
                .get("params");

        String script;
        ScriptType scriptType;
        final Object templateObj = sourceMap.get("template");
        if (templateObj instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> templateMap = (Map<String, Object>) templateObj;

            final String templateName = (String) templateMap.get("id");
            final String templateFile = (String) templateMap.get("file");
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
                    script = XContentFactory.jsonBuilder().value(templateMap)
                            .string();
                } catch (final IOException e) {
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

        searchRequest.source(new SearchTemplateChain(scriptService, filters
                .filters()).doCreate(lang, script, scriptType, paramMap));
        searchRequest.templateName(null);
        searchRequest.templateSource(null, false);
        searchRequest.templateType(null);

        return searchRequest;
    }

    @Override
    public void apply(final String action, final ActionResponse response,
            @SuppressWarnings("rawtypes") final ActionListener listener,
            final ActionFilterChain chain) {
        chain.proceed(action, response, listener);
    }

}
