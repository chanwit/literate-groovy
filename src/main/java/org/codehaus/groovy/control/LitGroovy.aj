package org.codehaus.groovy.control;

import org.codehaus.groovy.control.io.FileReaderSource;
import org.codehaus.groovy.control.io.ReaderSource;
import org.codehaus.groovy.control.io.StringReaderSource;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.RootNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public privileged aspect LitGroovy {

    pointcut nextPhase(SourceUnit su) :
        call(public void SourceUnit.nextPhase())
        && withincode(public void SourceUnit.parse())
        && this(su);

    before(SourceUnit su) : nextPhase(su) {
        final String name = su.getName();
        if(!(name.endsWith(".md.groovy") || name.endsWith(".md"))) {
            return;
        }

        try {
            Reader reader = su.getSource().getReader();
            BufferedReader br = new BufferedReader(reader);
            RootNode root;
            String text = IOGroovyMethods.getText(br);

            // Work around SHEBANG bug.
            // So the first line starts from 1, not 0.
            text = System.getProperty("line.separator") + text;

            char[] sources = text.toCharArray();
            root = new PegDownProcessor(Extensions.ALL).parseMarkdown(sources);
            LineMap lineMap = new LineMap(sources);
            String newSource = new NewSpockVisitor(lineMap, su).toSource(root);

            su.source = new StringReaderSource(newSource, su.getConfiguration());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
