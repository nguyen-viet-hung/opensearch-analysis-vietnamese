package org.opensearch.index.analysis;

import org.opensearch.action.admin.cluster.node.info.NodeInfo;
import org.opensearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.opensearch.action.admin.cluster.node.info.PluginsAndModules;
import org.opensearch.action.admin.indices.analyze.AnalyzeAction;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.plugin.analysis.vi.AnalysisVietnamesePlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.PluginInfo;
import org.opensearch.test.OpenSearchIntegTestCase;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.opensearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Created by duydo on 2/20/17.
 */
public class VietnameseAnalysisIntegrationTests extends OpenSearchIntegTestCase {
    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(AnalysisVietnamesePlugin.class);
    }

    public void testPluginIsLoaded() throws Exception {
        NodesInfoResponse response = client().admin().cluster().prepareNodesInfo().get();
        for (NodeInfo nodeInfo : response.getNodes()) {
            boolean pluginFound = false;
            for (PluginInfo pluginInfo : nodeInfo.getInfo(PluginsAndModules.class).getPluginInfos()) {
                if (pluginInfo.getName().equals(AnalysisVietnamesePlugin.class.getName())) {
                    pluginFound = true;
                    break;
                }
            }
            assertThat(pluginFound, is(true));
        }
    }

    public void testVietnameseAnalyzer() throws ExecutionException, InterruptedException {

        AnalyzeAction.Response response = client().admin().indices()
                .prepareAnalyze("công nghệ thông tin Việt Nam").setAnalyzer("vi_analyzer")
                .execute().get();
        String[] expected = {"công nghệ", "thông tin", "việt nam"};
        assertThat(response, notNullValue());
        assertThat(response.getTokens().size(), is(3));
        for (int i = 0; i < expected.length; i++) {
            assertThat(response.getTokens().get(i).getTerm(), is(expected[i]));
        }
    }

    public void testVietnameseAnalyzerInMapping() throws ExecutionException, InterruptedException, IOException {
        createIndex("test");
        ensureGreen("test");
        final XContentBuilder mapping = jsonBuilder()
                .startObject()
                    .startObject("_doc")
                        .startObject("properties")
                            .startObject("foo")
                                .field("type", "text")
                                .field("analyzer", "vi_analyzer")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
        client().admin().indices().preparePutMapping("test").setSource(mapping).get();
        final XContentBuilder source = jsonBuilder()
                .startObject()
                    .field("foo", "công nghệ thông tin Việt Nam")
                .endObject();
        index("test", "_doc", "1", source);
        refresh();
        SearchResponse response = client().prepareSearch("test").
                setQuery(
                    QueryBuilders.matchQuery("foo", "công nghệ thông tin")
                ).execute().actionGet();
        assertThat(response.getHits().getTotalHits().toString(), is("1 hits"));
    }
}
