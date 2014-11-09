package org.codelibs.elasticsearch.sstmpl;

import java.util.Collection;

import org.codelibs.elasticsearch.sstmpl.filter.SearchActionFilter;
import org.codelibs.elasticsearch.sstmpl.module.SearchTemplateModule;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;

/**
 * Script-based Search Template Plugin.
 *
 * @author shinsuke
 *
 */
public class ScriptTemplatePlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "ScriptTemplatePlugin";
    }

    @Override
    public String description() {
        return "This plugin provides Script-based Search Template.";
    }

    public void onModule(final ActionModule module) {
        module.registerFilter(SearchActionFilter.class);
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        final Collection<Class<? extends Module>> modules = Lists
                .newArrayList();
        modules.add(SearchTemplateModule.class);
        return modules;
    }
}
