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

package org.codelibs.elasticsearch.sstmpl.rest;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;

import java.io.IOException;
import java.util.*;

import org.codelibs.elasticsearch.sstmpl.action.MultiSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.MultiSearchScriptTemplateRequest;
import org.codelibs.elasticsearch.sstmpl.action.SearchScriptTemplateRequest;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.rest.action.search.RestMultiSearchAction;
import org.elasticsearch.rest.action.search.RestSearchAction;

public class RestMultiSearchScriptTemplateAction extends BaseRestHandler {

    private static final Set<String> RESPONSE_PARAMS;

    static {
        final Set<String> responseParams = new HashSet<>(
            Arrays.asList(RestSearchAction.TYPED_KEYS_PARAM, RestSearchAction.TOTAL_HITS_AS_INT_PARAM)
        );
        RESPONSE_PARAMS = Collections.unmodifiableSet(responseParams);
    }

    private final boolean allowExplicitIndex;

    public RestMultiSearchScriptTemplateAction(final Settings settings, final RestController controller) {
        this.allowExplicitIndex = MULTI_ALLOW_EXPLICIT_INDEX.get(settings);
    }

    @Override
    public String getName() {
        return "multi_search_script_template_action";
    }

    @Override
    public List<Route> routes() {
        return Collections.unmodifiableList(Arrays.asList(
                new Route(GET, "/_msearch/script_template"),
                new Route(POST, "/_msearch/script_template"),
                new Route(GET, "/{index}/_msearch/script_template"),
                new Route(POST, "/{index}/_msearch/script_template"),
                new Route(GET, "/{index}/{type}/_msearch/script_template"),
                new Route(POST, "/{index}/{type}/_msearch/script_template")
        ));
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        final MultiSearchScriptTemplateRequest multiRequest = parseRequest(request, allowExplicitIndex);
        return channel -> client.execute(MultiSearchScriptTemplateAction.INSTANCE, multiRequest, new RestToXContentListener<>(channel));
    }

    /**
     * Parses a {@link RestRequest} body and returns a {@link MultiSearchScriptTemplateRequest}
     */
    public static MultiSearchScriptTemplateRequest parseRequest(final RestRequest restRequest, final boolean allowExplicitIndex)
            throws IOException {
        final MultiSearchScriptTemplateRequest multiRequest = new MultiSearchScriptTemplateRequest();
        if (restRequest.hasParam("max_concurrent_searches")) {
            multiRequest.maxConcurrentSearchRequests(restRequest.paramAsInt("max_concurrent_searches", 0));
        }

        RestMultiSearchAction.parseMultiLineRequest(restRequest, multiRequest.indicesOptions(), allowExplicitIndex,
                (searchRequest, bytes) -> {
                    try {
                        final SearchScriptTemplateRequest searchTemplateRequest = RestSearchScriptTemplateAction.parse(bytes);
                        if (searchTemplateRequest.getScript() != null) {
                            searchTemplateRequest.setRequest(searchRequest);
                            multiRequest.add(searchTemplateRequest);
                        } else {
                            throw new IllegalArgumentException("Malformed search template");
                        }
                    } catch (final IOException e) {
                        throw new ElasticsearchParseException("Exception when parsing search template request", e);
                    }
                });
        return multiRequest;
    }

    @Override
    public boolean supportsContentStream() {
        return true;
    }

    @Override
    protected Set<String> responseParams() {
        return RESPONSE_PARAMS;
    }
}
