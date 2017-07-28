package org.codelibs.elasticsearch.sstmpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.codelibs.elasticsearch.sstmpl.action.MultiSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.SearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.TransportMultiSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.action.TransportSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.filter.SearchActionFilter;
import org.codelibs.elasticsearch.sstmpl.module.SearchTemplateModule;
import org.codelibs.elasticsearch.sstmpl.rest.RestDeleteSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.rest.RestGetSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.rest.RestMultiSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.rest.RestPutSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.rest.RestRenderSearchScriptTemplateAction;
import org.codelibs.elasticsearch.sstmpl.rest.RestSearchScriptTemplateAction;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.mustache.MustacheScriptEngineService;

/**
 * Script-based Search Template Plugin.
 *
 * @author shinsuke
 *
 */
public class ScriptTemplatePlugin extends Plugin implements ScriptPlugin, ActionPlugin {

    @Override
    public List<Class<? extends ActionFilter>> getActionFilters() {
        return Arrays.asList(SearchActionFilter.class);
    }

    @Override
    public Collection<Module> createGuiceModules() {
        return Arrays.asList(new SearchTemplateModule());
    }

    @Override
    public ScriptEngineService getScriptEngineService(Settings settings) {
        return new MustacheScriptEngineService();
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Arrays.asList(new ActionHandler<>(SearchScriptTemplateAction.INSTANCE, TransportSearchScriptTemplateAction.class),
                new ActionHandler<>(MultiSearchScriptTemplateAction.INSTANCE, TransportMultiSearchScriptTemplateAction.class));
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
            IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter, IndexNameExpressionResolver indexNameExpressionResolver,
            Supplier<DiscoveryNodes> nodesInCluster) {
        return Arrays.asList(new RestSearchScriptTemplateAction(settings, restController),
                new RestMultiSearchScriptTemplateAction(settings, restController), new RestGetSearchScriptTemplateAction(settings, restController),
                new RestPutSearchScriptTemplateAction(settings, restController), new RestDeleteSearchScriptTemplateAction(settings, restController),
                new RestRenderSearchScriptTemplateAction(settings, restController));
    }
}
