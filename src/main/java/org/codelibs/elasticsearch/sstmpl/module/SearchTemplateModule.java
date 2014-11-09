package org.codelibs.elasticsearch.sstmpl.module;

import java.util.List;

import org.codelibs.elasticsearch.sstmpl.filter.SearchTemplateFilter;
import org.codelibs.elasticsearch.sstmpl.filter.SearchTemplateFilters;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.Multibinder;

public class SearchTemplateModule extends AbstractModule {
    private final List<Class<? extends SearchTemplateFilter>> searchTemplateFilters = Lists
            .newArrayList();

    @Override
    protected void configure() {
        final Multibinder<SearchTemplateFilter> searchTemplateFilterMultibinder = Multibinder
                .newSetBinder(binder(), SearchTemplateFilter.class);
        for (final Class<? extends SearchTemplateFilter> searchTemplateFilter : searchTemplateFilters) {
            searchTemplateFilterMultibinder.addBinding().to(
                    searchTemplateFilter);
        }
        bind(SearchTemplateFilters.class).asEagerSingleton();
    }

    public SearchTemplateModule registerSearchTemplateFillter(
            final Class<? extends SearchTemplateFilter> searchTemplateFilter) {
        searchTemplateFilters.add(searchTemplateFilter);
        return this;
    }
}