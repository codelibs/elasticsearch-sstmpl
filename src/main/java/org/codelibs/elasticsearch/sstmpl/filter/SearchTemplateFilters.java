package org.codelibs.elasticsearch.sstmpl.filter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import org.elasticsearch.common.inject.Inject;

public class SearchTemplateFilters {

    private final SearchTemplateFilter[] filters;

    @Inject
    public SearchTemplateFilters(
            final Set<SearchTemplateFilter> searchTemplateFilters) {
        filters = searchTemplateFilters
                .toArray(new SearchTemplateFilter[searchTemplateFilters.size()]);
        Arrays.sort(filters, new Comparator<SearchTemplateFilter>() {
            @Override
            public int compare(final SearchTemplateFilter o1,
                    final SearchTemplateFilter o2) {
                return Integer.compare(o1.order(), o2.order());
            }
        });
    }

    /**
     * Returns the action filters that have been injected
     */
    public SearchTemplateFilter[] filters() {
        return filters;
    }

}
