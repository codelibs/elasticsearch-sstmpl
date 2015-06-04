Elasticsearch Script-based Search Template Plugin
=======================

## Overview

This plugin provides Script-based Search Template.
Elasticsearch has [Search Template](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-template.html "Search Template") which is Mustache-based template.
Since Mustache is Logic-less templates, you cannot put any logic, such as if-statement, into a search template.
This plugin executes a script-based search template to create a search query.
Therefore, you can use any script you want in Search Template.

## Version

| Version   | Elasticsearch |
|:---------:|:-------------:|
| master    | 1.5.X         |
| 1.5.2     | 1.5.2         |
| 1.4.2     | 1.4.0         |

### Issues/Questions

Please file an [issue](https://github.com/codelibs/elasticsearch-sstmpl/issues "issue").
(Japanese forum is [here](https://github.com/codelibs/codelibs-ja-forum "here").)

## Installation

### Install Script-based Search Template Plugin

    $ $ES_HOME/bin/plugin --install org.codelibs/elasticsearch-sstmpl/1.5.2

## References

Usages are similar to Elasticsearch's Search Template.
In this plugin, to use Script-based Search Template, "lang" property is added.

For examples in this page, groovy is used as a script language.
If you want to use other language, please add a script feature(plugin) to your Elasticsearch.
We also provides [Handlebars Lang Plugin](https://github.com/codelibs/elasticsearch-lang-handlebars "Handlebars Lang Plugin").

### Filling in a query string with a single value

    GET /_search/template
    {
        "lang": "groovy",
        "template": "'{\"query\": {\"match\": {\"title\": \"' + query_string + '\"}}}'",
        "params": {
            "query_string": "search for these words"
        }
    }

The value of template property is executed as a groovy script.

### File-based pre-registered template

You can register search templates by storing it in the config/scripts directory.
If you use a groovy script, the script file is below:

    $ echo "'{\"query\": {\"match\": {\"title\": \"' + query_string + '\"}}}'" > $ES_HOME/config/scripts/storedTemplate.groovy

In order to execute the stored template, reference it by it’s name under the template key:

    GET /_search/template
    {
        "lang": "groovy",
        "template": {
            "file": "storedTemplate"
        },
        "params": {
            "query_string": "search for these words"
        }
    }

### Index-based pre-registered template

This plugin is able to call scripts in .script index to create a template.
To add a search template as a script,

    POST /_scripts/groovy/search_query_1
    {
        "script":"'{\"query\": {\"match\": {\"title\": \"' + query_string + '\"}}}'"
    }

and then the search request is:

    GET /_search/template
    {
        "lang": "groovy",
        "template": {
            "id": "search_query_1"
        },
        "params": {
            "query_string": "search for these words"
        }
    }

### Search Template Filter

You can add your code before creating this search template in your Elasticsearch's plugin.
To create the filter, it's created with SearchTemplateFilter and then is registered to SearchTemplateModule(call registerSearchTemplateFillter method).
The sample code is [here](https://github.com/codelibs/elasticsearch-sstmpl/blob/master/src/test/java/org/codelibs/elasticsearch/sstmpl/filter/Test1Filter.java "Test1Filter").
