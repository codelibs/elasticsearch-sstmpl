package org.codelibs.elasticsearch.sstmpl.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.ScriptTemplateException;
import org.codelibs.elasticsearch.sstmpl.chain.SearchTemplateChain;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptService.ScriptType;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.threadpool.ThreadPool.Names;

public class SearchActionFilter extends AbstractComponent
        implements ActionFilter {

    private static final String SEARCH_REQUEST_INVOKED = "filter.codelibs.sstmpl.Invoked";

    private static final String PARAMS = "params";

    private static final String REQUEST = "request";

    private int order;

    private final ScriptService scriptService;

    private final SearchTemplateFilters filters;

    private final ThreadPool threadPool;

    @Inject
    public SearchActionFilter(final Settings settings,
            final ScriptService scriptService,
            final SearchTemplateFilters filters, final ThreadPool threadPool) {
        super(settings);
        this.scriptService = scriptService;
        this.filters = filters;
        this.threadPool = threadPool;

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

        final SearchRequest searchRequest = (SearchRequest) request;
        final Boolean invoked = searchRequest.getHeader(SEARCH_REQUEST_INVOKED);
        if (invoked != null && invoked.booleanValue()) {
            threadPool.executor(Names.SEARCH).execute(new Runnable() {
                @Override
                public void run() {
                    final BytesReference templateSource = searchRequest
                            .templateSource();
                    if (templateSource != null && templateSource.length() > 0) {
                        try {
                            final XContentParser parser = XContentFactory
                                    .xContent(templateSource)
                                    .createParser(templateSource);
                            final Map<String, Object> sourceMap = parser
                                    .mapAndClose();
                            final Object langObj = sourceMap.get("lang");
                            if (langObj != null) {
                                chain.proceed(action,
                                        createScriptSearchRequest(searchRequest,
                                                langObj.toString(), sourceMap),
                                        listener);
                            } else {
                                chain.proceed(action, searchRequest, listener);
                            }
                        } catch (final Exception e) {
                            listener.onFailure(e);
                        }
                    } else {
                        chain.proceed(action, searchRequest, listener);
                    }
                }
            });
        } else {
            searchRequest.putHeader(SEARCH_REQUEST_INVOKED, Boolean.TRUE);
            chain.proceed(action, request, listener);
        }
    }

    private SearchRequest createScriptSearchRequest(
            final SearchRequest searchRequest, final String lang,
            final Map<String, Object> sourceMap) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> paramMap = sourceMap.containsKey(PARAMS)
                ? (Map<String, Object>) sourceMap.get(PARAMS)
                : new HashMap<String, Object>();
        if (!paramMap.containsKey(REQUEST)) {
            paramMap.put(REQUEST, searchRequest);
        }

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

        searchRequest.source(
                new SearchTemplateChain(scriptService, filters.filters())
                        .doCreate(lang, script, scriptType, paramMap));
        searchRequest.templateName(null);
        searchRequest.templateSource(BytesArray.EMPTY);
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
