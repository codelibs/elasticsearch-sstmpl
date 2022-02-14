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
import java.util.Arrays;
import java.util.Iterator;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

public class MultiSearchScriptTemplateResponse extends ActionResponse
        implements Iterable<MultiSearchScriptTemplateResponse.Item>, ToXContentObject {

    /**
     * A search template response item, holding the actual search template response, or an error message if it failed.
     */
    public static class Item implements Writeable {
        private SearchScriptTemplateResponse response;
        private Exception exception;

        Item() {
        }

        public Item(final SearchScriptTemplateResponse response, final Exception exception) {
            this.response = response;
            this.exception = exception;
        }

        /**
         * Is it a failed search?
         */
        public boolean isFailure() {
            return exception != null;
        }

        /**
         * The actual failure message, null if its not a failure.
         */
        @Nullable
        public String getFailureMessage() {
            return exception == null ? null : exception.getMessage();
        }

        /**
         * The actual search response, null if its a failure.
         */
        @Nullable
        public SearchScriptTemplateResponse getResponse() {
            return this.response;
        }

        public static Item readItem(final StreamInput in) throws IOException {
            return new Item(in);
        }

        public Item(final StreamInput in) throws IOException {
            if (in.readBoolean()) {
                this.response = new SearchScriptTemplateResponse(in);
            } else {
                exception = in.readException();
            }
        }

        @Override
        public void writeTo(final StreamOutput out) throws IOException {
            if (response != null) {
                out.writeBoolean(true);
                response.writeTo(out);
            } else {
                out.writeBoolean(false);
                out.writeException(exception);
            }
        }

        public Exception getFailure() {
            return exception;
        }
    }

    private Item[] items;

    MultiSearchScriptTemplateResponse() {
    }

    public MultiSearchScriptTemplateResponse(final Item[] items) {
        this.items = items;
    }

    @Override
    public Iterator<Item> iterator() {
        return Arrays.stream(items).iterator();
    }

    /**
     * The list of responses, the order is the same as the one provided in the request.
     */
    public Item[] getResponses() {
        return this.items;
    }

    public MultiSearchScriptTemplateResponse(final StreamInput in) throws IOException {
        super(in);
        items = new Item[in.readVInt()];
        for (int i = 0; i < items.length; i++) {
            items[i] = Item.readItem(in);
        }
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeVInt(items.length);
        for (final Item item : items) {
            item.writeTo(out);
        }
    }

    @Override
    public XContentBuilder toXContent(final XContentBuilder builder, final ToXContent.Params params) throws IOException {
        builder.startObject();
        builder.startArray(Fields.RESPONSES);
        for (final Item item : items) {
            if (item.isFailure()) {
                builder.startObject();
                ElasticsearchException.generateFailureXContent(builder, params, item.getFailure(), true);
                builder.endObject();
            } else {
                item.getResponse().toXContent(builder, params);
            }
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }

    static final class Fields {
        static final String RESPONSES = "responses";
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }
}
