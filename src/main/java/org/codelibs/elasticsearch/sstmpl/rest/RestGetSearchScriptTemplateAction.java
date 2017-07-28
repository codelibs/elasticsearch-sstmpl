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

import java.io.IOException;

import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptRequest;
import org.elasticsearch.action.admin.cluster.storedscripts.GetStoredScriptResponse;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.StoredScriptSource;

public class RestGetSearchScriptTemplateAction extends BaseRestHandler {

    public static final ParseField _ID_PARSE_FIELD = new ParseField("_id");

    public static final ParseField FOUND_PARSE_FIELD = new ParseField("found");

    public RestGetSearchScriptTemplateAction(Settings settings, RestController controller) {
        super(settings);

        controller.registerHandler(GET, "/_search/script_template/{id}", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, NodeClient client) throws IOException {
        String id = request.param("id");

        GetStoredScriptRequest getRequest = new GetStoredScriptRequest(id, Script.DEFAULT_TEMPLATE_LANG);

        return channel -> client.admin().cluster().getStoredScript(getRequest, new RestBuilderListener<GetStoredScriptResponse>(channel) {
            @Override
            public RestResponse buildResponse(GetStoredScriptResponse response, XContentBuilder builder) throws Exception {
                builder.startObject();
                builder.field(_ID_PARSE_FIELD.getPreferredName(), id);

                builder.field(StoredScriptSource.LANG_PARSE_FIELD.getPreferredName(), Script.DEFAULT_TEMPLATE_LANG);

                StoredScriptSource source = response.getSource();
                boolean found = source != null;
                builder.field(FOUND_PARSE_FIELD.getPreferredName(), found);

                if (found) {
                    builder.field(StoredScriptSource.TEMPLATE_PARSE_FIELD.getPreferredName(), source.getCode());
                }

                builder.endObject();

                return new BytesRestResponse(found ? RestStatus.OK : RestStatus.NOT_FOUND, builder);
            }
        });
    }
}
