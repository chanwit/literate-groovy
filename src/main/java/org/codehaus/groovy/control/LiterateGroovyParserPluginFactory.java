package org.codehaus.groovy.control;

public class LiterateGroovyParserPluginFactory extends ParserPluginFactory {

    @Override
    public ParserPlugin createParserPlugin() {
        return new LiterateGroovyPlugin();
    }

}
