package org.codelibs.elasticsearch.sstmpl;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ScriptTemplatePluginTest {
    ElasticsearchClusterRunner runner;

    @Before
    public void setUp() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.build(newConfigs().numOfNode(1).ramIndexStore());
        runner.ensureGreen();
    }

    @After
    public void tearDown() throws Exception {
        runner.close();
        runner.clean();
    }

    @Test
    public void test_search() throws Exception {

        assertThat(1, is(runner.getNodeSize()));

        final String index = "sample";
        final String type = "data";
        runner.createIndex(index, ImmutableSettings.builder().build());

        for (int i = 1; i <= 1000; i++) {
            final IndexResponse indexResponse = runner.insert(index, type,
                    String.valueOf(i), "{\"id\":\"" + i + "\",\"msg\":\"test "
                            + i + "\",\"counter\":" + i + ",\"category\":"
                            + (i % 100) + "}");
            assertTrue(indexResponse.isCreated());
        }

        Node node = runner.node();

        String query;

        query = "{\"query\":{\"match_all\":{}}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search").body(query)
                .execute()) {
            Map<String, Object> contentMap = curlResponse.getContentAsMap();
            Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(1000, is(hitsMap.get("total")));
            assertThat(
                    10,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"template\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"msg\",\"my_value\":\"test\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/template")
                .body(query).execute()) {
            Map<String, Object> contentMap = curlResponse.getContentAsMap();
            Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(1000, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

        query = "{\"lang\":\"mustache\",\"template\":{\"query\":{\"match\":{\"{{my_field}}\":\"{{my_value}}\"}},\"size\":\"{{my_size}}\"},"
                + "\"params\":{\"my_field\":\"msg\",\"my_value\":\"test\",\"my_size\":\"50\"}}";
        try (CurlResponse curlResponse = Curl
                .post(node, "/" + index + "/" + type + "/_search/template")
                .body(query).execute()) {
            Map<String, Object> contentMap = curlResponse.getContentAsMap();
            Map<String, Object> hitsMap = (Map<String, Object>) contentMap
                    .get("hits");
            assertThat(1000, is(hitsMap.get("total")));
            assertThat(
                    50,
                    is(((List<Map<String, Object>>) hitsMap.get("hits")).size()));
        }

    }
}
