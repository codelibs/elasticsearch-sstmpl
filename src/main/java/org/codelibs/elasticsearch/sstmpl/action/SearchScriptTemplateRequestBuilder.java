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

import java.util.Map;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.script.ScriptType;

public class SearchScriptTemplateRequestBuilder
        extends ActionRequestBuilder<SearchScriptTemplateRequest, SearchScriptTemplateResponse, SearchScriptTemplateRequestBuilder> {

    SearchScriptTemplateRequestBuilder(final ElasticsearchClient client, final SearchScriptTemplateAction action) {
        super(client, action, new SearchScriptTemplateRequest());
    }

    public SearchScriptTemplateRequestBuilder(final ElasticsearchClient client) {
        this(client, SearchScriptTemplateAction.INSTANCE);
    }

    public SearchScriptTemplateRequestBuilder setRequest(final SearchRequest searchRequest) {
        request.setRequest(searchRequest);
        return this;
    }

    public SearchScriptTemplateRequestBuilder setSimulate(final boolean simulate) {
        request.setSimulate(simulate);
        return this;
    }

    /**
     * Enables explanation for each hit on how its score was computed. Disabled by default
     */
    public SearchScriptTemplateRequestBuilder setExplain(final boolean explain) {
        request.setExplain(explain);
        return this;
    }

    /**
     * Enables profiling of the query. Disabled by default
     */
    public SearchScriptTemplateRequestBuilder setProfile(final boolean profile) {
        request.setProfile(profile);
        return this;
    }

    public SearchScriptTemplateRequestBuilder setScriptType(final ScriptType scriptType) {
        request.setScriptType(scriptType);
        return this;
    }

    public SearchScriptTemplateRequestBuilder setScript(final String script) {
        request.setScript(script);
        return this;
    }

    public SearchScriptTemplateRequestBuilder setScriptParams(final Map<String, Object> scriptParams) {
        request.setScriptParams(scriptParams);
        return this;
    }
}
