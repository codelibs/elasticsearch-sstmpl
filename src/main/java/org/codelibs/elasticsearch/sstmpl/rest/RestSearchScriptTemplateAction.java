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
import java.util.Collections;
import java.util.Set;

import org.codelibs.elasticsearch.sstmpl.action.SearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.SearchScriptTemplateRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.rest.action.search.RestSearchAction;
import org.elasticsearch.script.ScriptType;

public class RestSearchScriptTemplateAction extends BaseRestHandler {

    private static final Set<String> RESPONSE_PARAMS = Collections.singleton(RestSearchAction.TYPED_KEYS_PARAM);

    private static ParseField ID_FIELD = new ParseField("id");
    private static ParseField SOURCE_FIELD = new ParseField("source", "inline", "template");

    private static ParseField PARAMS_FIELD = new ParseField("params");
    private static ParseField EXPLAIN_FIELD = new ParseField("explain");
    private static ParseField PROFILE_FIELD = new ParseField("profile");

    private static final ObjectParser<SearchScriptTemplateRequest, Void> PARSER;

    static {
        PARSER = new ObjectParser<>("search_template");
        PARSER.declareField((parser, request, s) -> request.setScriptParams(parser.map()), PARAMS_FIELD,
                ObjectParser.ValueType.OBJECT);
        PARSER.declareString((request, s) -> {
            request.setScriptType(ScriptType.STORED);
            request.setScript(s);
            request.setScriptLang(null);
        }, ID_FIELD);
        PARSER.declareString((request, s) -> {
            request.setScriptLang(s);
        }, new ParseField("lang"));
        PARSER.declareBoolean(SearchScriptTemplateRequest::setExplain, EXPLAIN_FIELD);
        PARSER.declareBoolean(SearchScriptTemplateRequest::setProfile, PROFILE_FIELD);
        PARSER.declareField((parser, request, value) -> {
            request.setScriptType(ScriptType.INLINE);
            if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
                //convert the template to json which is the only supported XContentType (see CustomMustacheFactory#createEncoder)
                try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
                    request.setScript(Strings.toString(builder.copyCurrentStructure(parser)));
                } catch (final IOException e) {
                    throw new ParsingException(parser.getTokenLocation(), "Could not parse inline template", e);
                }
            } else {
                request.setScript(parser.text());
            }
        }, SOURCE_FIELD, ObjectParser.ValueType.OBJECT_OR_STRING);
    }

    public RestSearchScriptTemplateAction(final Settings settings, final RestController controller) {
        controller.registerHandler(GET, "/_search/script_template", this);
        controller.registerHandler(POST, "/_search/script_template", this);
        controller.registerHandler(GET, "/{index}/_search/script_template", this);
        controller.registerHandler(POST, "/{index}/_search/script_template", this);
        controller.registerHandler(GET, "/{index}/{type}/_search/script_template", this);
        controller.registerHandler(POST, "/{index}/{type}/_search/script_template", this);
    }

    @Override
    public String getName() {
        return "search_script_template_action";
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        // Creates the search request with all required params
        final SearchRequest searchRequest = new SearchRequest();
        RestSearchAction.parseSearchRequest(searchRequest, request, null, size -> searchRequest.source().size(size));

        // Creates the search template request
        SearchScriptTemplateRequest searchTemplateRequest;
        try (XContentParser parser = request.contentOrSourceParamParser()) {
            searchTemplateRequest = parse(parser);
        }
        searchTemplateRequest.setRequest(searchRequest);

        return channel -> client.execute(SearchScriptTemplateAction.INSTANCE, searchTemplateRequest,
                new RestStatusToXContentListener<>(channel));
    }

    public static SearchScriptTemplateRequest parse(final XContentParser parser) throws IOException {
        return PARSER.parse(parser, new SearchScriptTemplateRequest(), null);
    }

    @Override
    protected Set<String> responseParams() {
        return RESPONSE_PARAMS;
    }
}
