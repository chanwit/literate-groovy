package org.codehaus.groovy.control;

import org.codehaus.groovy.antlr.AntlrParserPlugin;
import org.codehaus.groovy.antlr.SourceBuffer;
import org.codehaus.groovy.control.io.FileReaderSource;
import org.codehaus.groovy.control.messages.SimpleMessage;
import org.codehaus.groovy.runtime.IOGroovyMethods;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.RootNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class LiterateGroovyPlugin extends AntlrParserPlugin {

    protected void transformCSTIntoAST(SourceUnit sourceUnit, Reader reader, SourceBuffer sourceBuffer) throws CompilationFailedException {

        BufferedReader br = new BufferedReader(reader);
        RootNode root;
        SourceUnit newSourceUnit = null;
        ErrorCollector errorCollector = sourceUnit.getErrorCollector();
        try {
            String text = IOGroovyMethods.getText(br);

            // Work around SHEBANG bug.
            // So the first line starts from 1, not 0.
            text = "\n" + text;

            String name = sourceUnit.getName();
            char[] sources = text.toCharArray();
            root = new PegDownProcessor(Extensions.ALL).parseMarkdown(sources);
            LineMap lineMap = new LineMap(sources);
            String newSource = new NewSpockVisitor(lineMap).toSource(root);

            if (sourceUnit.getSource() instanceof FileReaderSource) {
                name = ((FileReaderSource)sourceUnit.getSource()).getFile().getAbsolutePath();
            }
            newSourceUnit = new SourceUnit(name, newSource, sourceUnit.getConfiguration(), sourceUnit.getClassLoader(), errorCollector);
            super.transformCSTIntoAST(newSourceUnit, newSourceUnit.getSource().getReader(), sourceBuffer);
        } catch (IOException e) {
            errorCollector.addFatalError(new SimpleMessage(e.getMessage(), newSourceUnit));
        }

    }

}
