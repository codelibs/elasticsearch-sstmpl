package org.codelibs.elasticsearch.sstmpl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.codelibs.elasticsearch.sstmpl.action.MultiSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.SearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.TransportMultiSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.TransportSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.rest.RestMultiSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.rest.RestRenderSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.rest.RestSearchScriptTemplateAction;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;

/**
 * Script-based Search Template Plugin.
 *
 * @author shinsuke
 *
 */
public class ScriptTemplatePlugin extends Plugin implements ActionPlugin {

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Arrays.asList(//
                new ActionHandler<>(SearchScriptTemplateAction.INSTANCE, TransportSearchScriptTemplateAction.class), //
                new ActionHandler<>(MultiSearchScriptTemplateAction.INSTANCE, TransportMultiSearchScriptTemplateAction.class));
    }

    @Override
    public List<RestHandler> getRestHandlers(final Settings settings, final RestController restController,
            final ClusterSettings clusterSettings, final IndexScopedSettings indexScopedSettings, final SettingsFilter settingsFilter,
            final IndexNameExpressionResolver indexNameExpressionResolver, final Supplier<DiscoveryNodes> nodesInCluster) {
        return Arrays.asList(//
                new RestSearchScriptTemplateAction(settings, restController), //
                new RestMultiSearchScriptTemplateAction(settings, restController), //
                new RestRenderSearchScriptTemplateAction(settings, restController));
    }
}
