package org.codelibs.elasticsearch.sstmpl;

import java.util.ArrayList;
import java.util.Collection;

import org.codelibs.elasticsearch.sstmpl.filter.SearchActionFilter;
import org.codelibs.elasticsearch.sstmpl.module.SearchTemplateModule;
import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;

/**
 * Script-based Search Template Plugin.
 *
 * @author shinsuke
 *
 */
public class ScriptTemplatePlugin extends Plugin {

    @Override
    public String name() {
        return "sstmpl";
    }

    @Override
    public String description() {
        return "This plugin provides Script-based Search Template.";
    }

    public void onModule(final ActionModule module) {
        module.registerFilter(SearchActionFilter.class);
    }

    @Override
    public Collection<Module> nodeModules() {
        final Collection<Module> modules = new ArrayList<>();
        modules.add(new SearchTemplateModule());
        return modules;
    }
}
