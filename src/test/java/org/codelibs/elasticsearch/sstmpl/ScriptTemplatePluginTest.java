package org.codelibs.elasticsearch.sstmpl;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.codelibs.curl.CurlResponse;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.EcrCurl;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScriptTemplatePluginTest {
    ElasticsearchClusterRunner runner;

    private File esHomeDir;

    private String clusterName;

    @Before
    public void setUp() throws Exception {
        clusterName = "es-sstmpl-" + System.currentTimeMillis();
        esHomeDir = File.createTempFile("eshome", "");
        esHomeDir.delete();

        runner = new ElasticsearchClusterRunner();
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("http.cors.allow-origin", "*");
                settingsBuilder.putList("discovery.zen.ping.unicast.hosts", "localhost:9301-9310");
            }
        }).build(newConfigs().clusterName(clusterName).numOfNode(1).basePath(esHomeDir.getAbsolutePath()).pluginTypes(
                "org.codelibs.elasticsearch.sstmpl.ScriptTemplatePlugin"));
        runner.ensureGreen();
    }

    @After
    public void tearDown() throws Exception {
        runner.close();
        esHomeDir.delete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_search() throws Exception {

        assertThat(1, is(runner.getNodeSize()));

        final Node node = runner.node();

        final String index = "sample";
        final String type = "_doc";
        runner.createIndex(index, Settings.builder().build());

        for (int i = 1; i <= 1000; i++) {
            final IndexResponse indexResponse = runner.insert(index, type,
                    String.valueOf(i), "{\"id\":\"" + i + "\",\"msg\":\"test "
                            + i + "\",\"counter\":" + i + ",\"category\":" + i
                            % 10 + "}");
            assertEquals(Result.CREATED, indexResponse.getResult());
        }

        String query;

        query = "{\"script\":{\"lang\":\"mustache\",\"source\":"//
                + "{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"}"//
                + "}}";
        try (CurlResponse curlResponse =
                EcrCurl.post(node, "/_scripts/search_query_1").header("Content-Type", "application/json").body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse.getContent(EcrCurl.jsonParser);
            assertThat(true, is(contentMap.get("acknowledged")));
        }

        query = "{\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = EcrCurl
                .post(node, "/" + index + "/" + type + "/_search/script_template")
                .header("Content-Type", "application/json")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContent(EcrCurl.jsonParser);
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"query\":{\"match_all\":{}}}";
        try (CurlResponse curlResponse = EcrCurl
                .post(node, "/" + index + "/" + type + "/_search").body(query)
                .header("Content-Type", "application/json")
                .execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContent(EcrCurl.jsonParser);
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(1000, is(hitsMap.get("total")));
            assertThat(
                    10,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = EcrCurl.post(node, "/" + index + "/" + type + "/_search/script_template").body(query)
                .header("Content-Type", "application/json").execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContent(EcrCurl.jsonParser);
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"mustache\",\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = EcrCurl.post(node, "/" + index + "/" + type + "/_search/script_template").body(query)
                .header("Content-Type", "application/json").execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContent(EcrCurl.jsonParser);
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"id\":\"search_query_1\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = EcrCurl.post(node, "/" + index + "/" + type + "/_search/script_template").body(query)
                .header("Content-Type", "application/json").execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContent(EcrCurl.jsonParser);
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void test_render() throws Exception {

        assertThat(1, is(runner.getNodeSize()));

        final Node node = runner.node();

        String query;

        query = "{\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse =
                EcrCurl.post(node, "/_render/script_template").body(query).header("Content-Type", "application/json").execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContent(EcrCurl.jsonParser);
            final Map<String, Object> queryMap = (Map<String, Object>) contentMap
                    .get("template_output");
            assertThat("50", is(queryMap.get("size").toString()));
            assertThat("{match={category=1}}", is(queryMap.get("query").toString()));
        }
    }
}
