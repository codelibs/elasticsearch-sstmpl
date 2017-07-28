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

import static org.codelibs.elasticsearch.sstmpl.action.TransportSearchScriptTemplateAction.convert;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.TransportMultiSearchAction;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportMultiSearchScriptTemplateAction extends HandledTransportAction<MultiSearchScriptTemplateRequest, MultiSearchScriptTemplateResponse> {

    private final ScriptService scriptService;
    private final NamedXContentRegistry xContentRegistry;
    private final TransportMultiSearchAction multiSearchAction;

    @Inject
    public TransportMultiSearchScriptTemplateAction(Settings settings, ThreadPool threadPool, TransportService transportService,
                                              ActionFilters actionFilters, IndexNameExpressionResolver resolver,
                                              ScriptService scriptService, NamedXContentRegistry xContentRegistry,
                                              TransportMultiSearchAction multiSearchAction) {
        super(settings, MultiSearchScriptTemplateAction.NAME, threadPool, transportService, actionFilters, resolver,
                MultiSearchScriptTemplateRequest::new);
        this.scriptService = scriptService;
        this.xContentRegistry = xContentRegistry;
        this.multiSearchAction = multiSearchAction;
    }

    @Override
    protected void doExecute(MultiSearchScriptTemplateRequest request, ActionListener<MultiSearchScriptTemplateResponse> listener) {
        List<Integer> originalSlots = new ArrayList<>();
        MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
        multiSearchRequest.indicesOptions(request.indicesOptions());
        if (request.maxConcurrentSearchRequests() != 0) {
            multiSearchRequest.maxConcurrentSearchRequests(request.maxConcurrentSearchRequests());
        }

        MultiSearchScriptTemplateResponse.Item[] items = new MultiSearchScriptTemplateResponse.Item[request.requests().size()];
        for (int i = 0; i < items.length; i++) {
            SearchScriptTemplateRequest searchTemplateRequest = request.requests().get(i);
            SearchScriptTemplateResponse searchTemplateResponse = new SearchScriptTemplateResponse();
            SearchRequest searchRequest;
            try {
                searchRequest = convert(searchTemplateRequest, searchTemplateResponse, scriptService, xContentRegistry);
            } catch (Exception e) {
                items[i] = new MultiSearchScriptTemplateResponse.Item(null, e);
                continue;
            }
            items[i] = new MultiSearchScriptTemplateResponse.Item(searchTemplateResponse, null);
            if (searchRequest != null) {
                multiSearchRequest.add(searchRequest);
                originalSlots.add(i);
            }
        }

        multiSearchAction.execute(multiSearchRequest, ActionListener.wrap(r -> {
            for (int i = 0; i < r.getResponses().length; i++) {
                MultiSearchResponse.Item item = r.getResponses()[i];
                int originalSlot = originalSlots.get(i);
                if (item.isFailure()) {
                    items[originalSlot] = new MultiSearchScriptTemplateResponse.Item(null, item.getFailure());
                } else {
                    items[originalSlot].getResponse().setResponse(item.getResponse());
                }
            }
            listener.onResponse(new MultiSearchScriptTemplateResponse(items));
        }, listener::onFailure));
    }
}
