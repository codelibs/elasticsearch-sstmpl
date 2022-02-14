Elasticsearch Script-based Search Template Plugin
=======================

## Overview

This plugin provides Script-based Search Template.
Elasticsearch has [Search Template](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-template.html "Search Template") which is Mustache-based template.
Since Mustache is Logic-less templates, you cannot put any logic, such as if-statement, into a search template.
This plugin executes a script-based search template to create a search query.
Therefore, you can use any script you want in Search Template.

## Version

[Versions in Maven Repository](https://repo1.maven.org/maven2/org/codelibs/elasticsearch-sstmpl/)

### Issues/Questions

Please file an [issue](https://github.com/codelibs/elasticsearch-sstmpl/issues "issue").

## Installation

    $ $ES_HOME/bin/elasticsearch-plugin install org.codelibs:elasticsearch-sstmpl:7.16.3

## References

Usages are similar to Elasticsearch's Search Template.
In this plugin, to use Script-based Search Template, "lang" property is added.

For examples in this page, groovy is used as a script language.
If you want to use other language, please add a script feature(plugin) to your Elasticsearch.
We also provides [Handlebars Lang Plugin](https://github.com/codelibs/elasticsearch-lang-handlebars "Handlebars Lang Plugin").

### Filling in a query string with a single value

    GET /_search/script_template
    {
        "lang": "mustache",
        "inline": {"query":{"match":{"{{my_field}}":"{{my_value}}"}},"size":"{{my_size}}"},
        "params": {
            "my_field": "category",
            "my_value": "1",
            "my_size": "10"
        }
    }

The value of template property is executed as a groovy script.

### Index-based pre-registered template

This plugin is able to call scripts in .script index to create a template.
To add a search template as a script,

    POST /_script/search_query_1
    {
        "script": {
	    "lang": "mustache",
            "source": {"query":{"match":{"{{my_field}}":"{{my_value}}"}},"size":"{{my_size}}"}
	}
    }

and then the search request is:

    GET /_search/script_template
    {
        "id": "search_query_1",
        "params": {
            "my_field": "category",
            "my_value": "1",
            "my_size": "10"
        }
    }

