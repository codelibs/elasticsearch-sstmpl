package org.codelibs.elasticsearch.sstmpl;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

public class ScriptTemplatePluginTest {
    ElasticsearchClusterRunner runner;

    private File esHomeDir;

    private String clusterName;

    @Before
    public void setUp() throws Exception {
        clusterName = "es-sstmpl-" + System.currentTimeMillis();
        esHomeDir = File.createTempFile("eshome", "");
        esHomeDir.delete();

        final File scriptDir = new File(esHomeDir, "config/node_1/scripts");
        scriptDir.mkdirs();
        final File scriptFile = new File(scriptDir, "search_query_2.groovy");
        Files.write(
                "'{\"query\":{\"match\":{\"'+my_field+'\":\"'+my_value+'\"}},\"size\":\"'+my_size+'\"}'"
                        .getBytes(), scriptFile);

        runner = new ElasticsearchClusterRunner();
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("script.inline", "on");
                settingsBuilder.put("script.stored", "on");
                settingsBuilder.put("script.file", "on");
                settingsBuilder.put("script.search", "on");
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("http.cors.allow-origin", "*");
                settingsBuilder.putArray("discovery.zen.ping.unicast.hosts", "localhost:9301-9310");
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

    @Test
    public void test_search() throws Exception {

        assertThat(1, is(runner.getNodeSize()));

        final Node node = runner.node();

        final String index = "sample";
        final String type = "data";
        runner.createIndex(index, Settings.builder().build());

        for (int i = 1; i <= 1000; i++) {
            final IndexResponse indexResponse = runner.insert(index, type,
                    String.valueOf(i), "{\"id\":\"" + i + "\",\"msg\":\"test "
                            + i + "\",\"counter\":" + i + ",\"category\":" + i
                            % 10 + "}");
            assertEquals(Result.CREATED, indexResponse.getResult());
        }

        try (CurlResponse curlResponse = Curl
                .post(node, "/_search/script_template/groovy/search_query_1")
                .body("{\"template\":\"'{\\\"query\\\":{\\\"match\\\":{\\\"'+my_field+'\\\":\\\"'+my_value+'\\\"}},\\\"size\\\":\\\"'+my_size+'\\\"}'\"}")
                .execute()) {
            assertThat(200, is(curlResponse.getHttpStatusCode()));
        }

        String query;

        query = "{\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"query\":{\"match_all\":{}}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search").body(query)
                .execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(1000, is(hitsMap.get("total")));
            assertThat(
                    10,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"mustache\",\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"inline\":\"'{\\\"query\\\":{\\\"match\\\":{\\\"'+my_field+'\\\":\\\"'+my_value+'\\\"}},\\\"size\\\":\\\"'+my_size+'\\\"}'\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"id\":\"search_query_1\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"file\":\"search_query_2\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"inline\":\"'{\\\"query\\\":{\\\"match\\\":{\\\"'+my_field+'\\\":\\\"'+my_value+'\\\"}},\\\"size\\\":\\\"'+my_size+'\\\"}'\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"inline\":\"'{\\\"query\\\":{\\\"match\\\":{\\\"'+my_field+'\\\":\\\"'+my_value+'\\\"}},\\\"size\\\":\\\"'+my_size+'\\\"}'\","
                + "\"params\":{\"my_fieldx\":\"category\",\"my_valuex\":\"1\",\"my_sizex\":\"50\"}}";
        try (CurlResponse curlResponse = Curl.post(node, "/" + index + "/" + type + "/_search/script_template").body(query).execute()) {
            assertThat(curlResponse.getHttpStatusCode(), is(500));
        }
    }


    @SuppressWarnings("unchecked")
    @Test
    public void test_render() throws Exception {

        assertThat(1, is(runner.getNodeSize()));

        final Node node = runner.node();

        try (CurlResponse curlResponse = Curl
                .post(node, "/_search/script_template/groovy/search_query_1")
                .body("{\"template\":\"'{\\\"query\\\":{\\\"match\\\":{\\\"'+my_field+'\\\":\\\"'+my_value+'\\\"}},\\\"size\\\":\\\"'+my_size+'\\\"}'\"}")
                .execute()) {
            assertThat(200, is(curlResponse.getHttpStatusCode()));
        }

        String query;

        query = "{\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/_render/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"query\":{\"match_all\":{}}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/_render").body(query)
                .execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(1000, is(hitsMap.get("total")));
            assertThat(
                    10,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/_render/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"mustache\",\"inline\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/_render/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"inline\":\"'{\\\"query\\\":{\\\"match\\\":{\\\"'+my_field+'\\\":\\\"'+my_value+'\\\"}},\\\"size\\\":\\\"'+my_size+'\\\"}'\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/_render/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"id\":\"search_query_1\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/_render/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"file\":\"search_query_2\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/_render/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"inline\":\"'{\\\"query\\\":{\\\"match\\\":{\\\"'+my_field+'\\\":\\\"'+my_value+'\\\"}},\\\"size\\\":\\\"'+my_size+'\\\"}'\","
                + "\"params\":{\"my_field\":\"category\",\"my_value\":\"1\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/_render/script_template")
                .body(query).execute()) {
            final Map<String, Object> contentMap = curlResponse
                    .getContentAsMap();
            final Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(100, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"groovy\",\"inline\":\"'{\\\"query\\\":{\\\"match\\\":{\\\"'+my_field+'\\\":\\\"'+my_value+'\\\"}},\\\"size\\\":\\\"'+my_size+'\\\"}'\","
                + "\"params\":{\"my_fieldx\":\"category\",\"my_valuex\":\"1\",\"my_sizex\":\"50\"}}";
        try (CurlResponse curlResponse = Curl.post(node, "/_render/script_template").body(query).execute()) {
            assertThat(curlResponse.getHttpStatusCode(), is(500));
        }
    }
}
