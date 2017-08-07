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

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.StatusToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

public class SearchScriptTemplateResponse extends ActionResponse implements StatusToXContentObject {

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
        super.writeTo(out);
        out.writeOptionalBytesReference(source);
        out.writeOptionalStreamable(response);
    }

    @Override
    public void readFrom(final StreamInput in) throws IOException {
        super.readFrom(in);
        source = in.readOptionalBytesReference();
        response = in.readOptionalStreamable(SearchResponse::new);
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final Params params) throws IOException {
        if (hasResponse()) {
            response.toXContent(builder, params);
        } else {
            builder.startObject();
            //we can assume the template is always json as we convert it before compiling it
            builder.rawField("template_output", source, XContentType.JSON);
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