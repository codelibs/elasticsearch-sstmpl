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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.StatusToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

public class SearchScriptTemplateResponse extends ActionResponse implements StatusToXContentObject {
    public static ParseField TEMPLATE_OUTPUT_FIELD = new ParseField("template_output");

    /** Contains the source of the rendered template **/
    private BytesReference source;

    /** Contains the search response, if any **/
    private SearchResponse response;

    SearchScriptTemplateResponse() {
    }

    public BytesReference getSource() {
        return source;
    }

    public void setSource(final BytesReference source) {
        this.source = source;
    }

    public SearchResponse getResponse() {
        return response;
    }

    public void setResponse(final SearchResponse searchResponse) {
        this.response = searchResponse;
    }

    public boolean hasResponse() {
        return response != null;
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeOptionalBytesReference(source);
        out.writeOptionalWriteable(response);
    }

    public static SearchScriptTemplateResponse fromXContent(XContentParser parser) throws IOException {
        SearchScriptTemplateResponse searchTemplateResponse = new SearchScriptTemplateResponse();
        Map<String, Object> contentAsMap = parser.map();

        if (contentAsMap.containsKey(TEMPLATE_OUTPUT_FIELD.getPreferredName())) {
            Object source = contentAsMap.get(TEMPLATE_OUTPUT_FIELD.getPreferredName());
            XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON)
                .value(source);
            searchTemplateResponse.setSource(BytesReference.bytes(builder));
        } else {
            XContentType contentType = parser.contentType();
            XContentBuilder builder = XContentFactory.contentBuilder(contentType)
                .map(contentAsMap);
            XContentParser searchResponseParser = contentType.xContent().createParser(
                parser.getXContentRegistry(),
                parser.getDeprecationHandler(),
                BytesReference.bytes(builder).streamInput());

            SearchResponse searchResponse = SearchResponse.fromXContent(searchResponseParser);
            searchTemplateResponse.setResponse(searchResponse);
        }
        return searchTemplateResponse;
    }

    public  SearchScriptTemplateResponse(final StreamInput in) throws IOException {
        super(in);
        source = in.readOptionalBytesReference();
        response = in.readOptionalWriteable(SearchResponse::new);
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final Params params) throws IOException {
        if (hasResponse()) {
            response.toXContent(builder, params);
        } else {
            builder.startObject();
            //we can assume the template is always json as we convert it before compiling it
            try (InputStream stream = source.streamInput()) {
                builder.rawField(TEMPLATE_OUTPUT_FIELD.getPreferredName(), stream, XContentType.JSON);
            }
            builder.endObject();
        }
        return builder;
    }

    @Override
    public RestStatus status() {
        if (hasResponse()) {
            return response.status();
        } else {
            return RestStatus.OK;
        }
    }
}
