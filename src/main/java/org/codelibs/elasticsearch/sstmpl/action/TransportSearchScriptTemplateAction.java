/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.elasticsearch.sstmpl.action;

import static org.elasticsearch.script.ScriptContext.Standard.SEARCH;

import java.io.IOException;
import java.util.Collections;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.TransportSearchAction;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryParseContext;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.mustache.MustacheScriptEngineService;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.template.CompiledTemplate;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportSearchScriptTemplateAction extends HandledTransportAction<SearchScriptTemplateRequest, SearchScriptTemplateResponse> {

    private static final String TEMPLATE_LANG = MustacheScriptEngineService.NAME;

    private final ScriptService scriptService;
    private final TransportSearchAction searchAction;
    private final NamedXContentRegistry xContentRegistry;

    @Inject
    public TransportSearchScriptTemplateAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                                         ActionFilters actionFilters, IndexNameExpressionResolver resolver,
                                         ScriptService scriptService,
                                         TransportSearchAction searchAction,
                                         NamedXContentRegistry xContentRegistry) {
        super(settings, SearchScriptTemplateAction.NAME, threadPool, transportService, actionFilters, resolver, SearchScriptTemplateRequest::new);
        this.scriptService = scriptService;
        this.searchAction = searchAction;
        this.xContentRegistry = xContentRegistry;
    }

    @Override
    protected void doExecute(SearchScriptTemplateRequest request, ActionListener<SearchScriptTemplateResponse> listener) {
        final SearchScriptTemplateResponse response = new SearchScriptTemplateResponse();
        try {
            SearchRequest searchRequest = convert(request, response, scriptService, xContentRegistry);
            if (searchRequest != null) {
                searchAction.execute(searchRequest, new ActionListener<SearchResponse>() {
                    @Override
                    public void onResponse(SearchResponse searchResponse) {
                        try {
                            response.setResponse(searchResponse);
                            listener.onResponse(response);
                        } catch (Exception t) {
                            listener.onFailure(t);
                        }
                    }

                    @Override
                    public void onFailure(Exception t) {
                        listener.onFailure(t);
                    }
                });
            } else {
                listener.onResponse(response);
            }
        } catch (IOException e) {
            listener.onFailure(e);
        }
    }

    static SearchRequest convert(SearchScriptTemplateRequest searchTemplateRequest, SearchScriptTemplateResponse response, ScriptService scriptService,
                                 NamedXContentRegistry xContentRegistry) throws IOException {
        Script script = new Script(searchTemplateRequest.getScriptType(), TEMPLATE_LANG, searchTemplateRequest.getScript(),
                searchTemplateRequest.getScriptParams() == null ? Collections.emptyMap() : searchTemplateRequest.getScriptParams());
        CompiledTemplate compiledScript = scriptService.compileTemplate(script, SEARCH);
        BytesReference source = compiledScript.run(script.getParams());
        response.setSource(source);

        SearchRequest searchRequest = searchTemplateRequest.getRequest();
        response.setSource(source);
        if (searchTemplateRequest.isSimulate()) {
            return null;
        }

        try (XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(xContentRegistry, source)) {
            SearchSourceBuilder builder = SearchSourceBuilder.searchSource();
            builder.parseXContent(new QueryParseContext(parser));
            builder.explain(searchTemplateRequest.isExplain());
            builder.profile(searchTemplateRequest.isProfile());
            searchRequest.source(builder);
        }
        return searchRequest;
    }
}
