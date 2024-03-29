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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codelibs.elasticsearch.sstmpl.action.SearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.SearchScriptTemplateRequest;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.script.ScriptType;

public class RestRenderSearchScriptTemplateAction extends BaseRestHandler {
    public RestRenderSearchScriptTemplateAction(final Settings settings, final RestController controller) {
    }

    @Override
    public String getName() {
        return "render_search_script_template_action";
    }

    @Override
    public List<Route> routes() {
        return Collections.unmodifiableList(Arrays.asList(
                new Route(GET, "/_render/script_template"),
                new Route(POST, "/_render/script_template"),
                new Route(GET, "/_render/script_template/{id}"),
                new Route(POST, "/_render/script_template/{id}")
        ));
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        // Creates the render template request
        SearchScriptTemplateRequest renderRequest;
        try (XContentParser parser = request.contentOrSourceParamParser()) {
            renderRequest = RestSearchScriptTemplateAction.parse(parser);
        }
        renderRequest.setSimulate(true);

        final String id = request.param("id");
        if (id != null) {
            renderRequest.setScriptType(ScriptType.STORED);
            renderRequest.setScript(id);
        }

        return channel -> client.execute(SearchScriptTemplateAction.INSTANCE, renderRequest, new RestToXContentListener<>(channel));
    }
}
