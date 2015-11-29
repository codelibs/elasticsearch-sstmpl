package org.codelibs.elasticsearch.sstmpl;

import org.codelibs.elasticsearch.sstmpl.filter.Test1Filter;
import org.codelibs.elasticsearch.sstmpl.filter.Test2Filter;
import org.codelibs.elasticsearch.sstmpl.filter.Test3Filter;
import org.codelibs.elasticsearch.sstmpl.module.SearchTemplateModule;
import org.elasticsearch.plugins.Plugin;

public class TestPugin extends Plugin {

    @Override
    public String name() {
        return "TestPugin";
    }

    @Override
    public String description() {
        return "This is a test plugin.";
    }

    public void onModule(final SearchTemplateModule module) {
        module.registerSearchTemplateFillter(Test3Filter.class);
        module.registerSearchTemplateFillter(Test2Filter.class);
        module.registerSearchTemplateFillter(Test1Filter.class);
    }
}
