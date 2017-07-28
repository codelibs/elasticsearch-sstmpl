package org.codelibs.elasticsearch.sstmpl.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.elasticsearch.sstmpl.ScriptTemplateException;
import org.codelibs.elasticsearch.sstmpl.chain.SearchTemplateChain;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilterChain;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.threadpool.ThreadPool.Names;

public class SearchActionFilter extends AbstractComponent
        implements ActionFilter {

    private static final String PARAMS = "params";

    private static final String REQUEST = "request";

    private int order;

    private final ScriptService scriptService;

    private final SearchTemplateFilters filters;

    private final ThreadPool threadPool;

    private final NamedXContentRegistry xContentRegistry;

    @Inject
    public SearchActionFilter(final Settings settings,
            final ScriptService scriptService,
            final SearchTemplateFilters filters, final ThreadPool threadPool, final NamedXContentRegistry xContentRegistry) {
        super(settings);
        this.scriptService = scriptService;
        this.filters = filters;
        this.threadPool = threadPool;
        this.xContentRegistry = xContentRegistry;

        order = settings.getAsInt("indices.sstmpl.filter.order", 1);
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public final <Request extends ActionRequest, Response extends ActionResponse> void apply(final Task task, final String action,
            final Request request, final ActionListener<Response> listener, final ActionFilterChain<Request, Response> chain) {
        if (!SearchAction.INSTANCE.name().equals(action)) {
            chain.proceed(task, action, request, listener);
            return;
        }

        final SearchRequest searchRequest = (SearchRequest) request;
        threadPool.executor(Names.SEARCH).execute(() -> {
            final BytesReference source = searchRequest.source().buildAsBytes();
            if (source != null && source.length() > 0) {
                try (final XContentParser parser = XContentFactory.xContent(source).createParser(xContentRegistry, source)) {
                    final Map<String, Object> sourceMap = parser.map();
                    final Object langObj = sourceMap.get("lang");
                    if (langObj != null) {
                        chain.proceed(task, action, createScriptSearchRequest(searchRequest, langObj.toString(), sourceMap), listener);
                    } else {
                        chain.proceed(task, action, request, listener);
                    }
                } catch (final Exception e) {
                    listener.onFailure(e);
                }
            } else {
                chain.proceed(task, action, request, listener);
            }
        });
    }

    private <Request extends ActionRequest> Request createScriptSearchRequest(
            final SearchRequest searchRequest, final String lang,
            final Map<String, Object> sourceMap) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> paramMap = sourceMap.containsKey(PARAMS)
                ? (Map<String, Object>) sourceMap.get(PARAMS)
                : new HashMap<String, Object>();
        if (!paramMap.containsKey(REQUEST)) {
            paramMap.put(REQUEST, searchRequest);
        }

        final Tuple<ScriptType, String> tuple = parseScript(sourceMap);
        final String script = tuple.v2();
        final ScriptType scriptType = tuple.v1();
        final String source = new SearchTemplateChain(scriptService, filters.filters()).doCreate(lang, script, scriptType, paramMap);
        try (XContentParser parser = XContentFactory.xContent(source).createParser(xContentRegistry, source)) {
            SearchSourceBuilder builder = SearchSourceBuilder.searchSource();
            builder.parseXContent(new QueryParseContext(parser));
            searchRequest.source(builder);
        } catch (final IOException e) {
            throw new ElasticsearchException("Could not parse script-template: {0}", e, source);
        }

        return (Request) searchRequest;
    }

    private Tuple<ScriptType, String> parseScript(Map<String, Object> sourceMap) {
        final Object inlineObj = sourceMap.get("inline");
        if (inlineObj instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> inlineMap = (Map<String, Object>) inlineObj;
            // query
            try {
                return new Tuple<>(ScriptType.INLINE, XContentFactory.jsonBuilder().value(inlineMap).string());
            } catch (final IOException e) {
                throw new ScriptTemplateException("Failed to parse inline object: " + inlineObj);
            }
        } else if (inlineObj instanceof String) {
            // query
            return new Tuple<>(ScriptType.INLINE, inlineObj.toString());
        }

        final Object fileObj = sourceMap.get("file");
        if (fileObj instanceof String) {
            // file
            return new Tuple<>(ScriptType.FILE, fileObj.toString());
        }

        final Object idObj = sourceMap.get("id");
        if (idObj instanceof String) {
            // id
            return new Tuple<>(ScriptType.STORED, idObj.toString());
        }

        final Object templateObj = sourceMap.get("template");
        if (templateObj instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> templateMap = (Map<String, Object>) templateObj;

            final String templateName = (String) templateMap.get("id");
            final String templateFile = (String) templateMap.get("file");
            if (templateName != null) {
                // id
                return new Tuple<>(ScriptType.STORED, templateName);
            } else if (templateFile != null) {
                // file
                return new Tuple<>(ScriptType.FILE, templateFile);
            } else {
                // query
                try {
                    return new Tuple<>(ScriptType.INLINE, XContentFactory.jsonBuilder().value(templateMap).string());
                } catch (final IOException e) {
                    throw new ScriptTemplateException("Failed to parse template object: " + templateObj);
                }
            }
        } else if (templateObj instanceof String) {
            // query
            return new Tuple<>(ScriptType.INLINE, templateObj.toString());
        } else {
            throw new ScriptTemplateException("template is not an object.");
        }
    }
}
