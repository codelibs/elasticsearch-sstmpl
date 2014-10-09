package org.codelibs.elasticsearch.sstmpl;

import org.codelibs.elasticsearch.sstmpl.filter.SearchActionFilter;
import org.elasticsearch.action.ActionModule;
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

}
