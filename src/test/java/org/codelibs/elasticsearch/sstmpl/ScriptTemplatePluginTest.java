package org.codelibs.elasticsearch.sstmpl;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
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
        final Client client = runner.client();

        final String index = "sample";
        final String type = "data";
        runner.createIndex(index, ImmutableSettings.builder().build());

        for (int i = 1; i <= 1000; i++) {
            final IndexResponse indexResponse = runner.insert(index, type,
                    String.valueOf(i), "{\"id\":\"" + i + "\",\"msg\":\"test "
                            + i + "\",\"counter\":" + i + "}");
            assertTrue(indexResponse.isCreated());
        }

    }
}
