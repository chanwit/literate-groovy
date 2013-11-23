package org.codehaus.groovy.control;

import org.pegdown.FastEncoder;

public class IndentPrinter {

    private final StringBuilder sb;
    private int indent = 0;

    public IndentPrinter() {
        this(new StringBuilder());
    }

    public IndentPrinter(StringBuilder sb) {
        this.sb = sb;
    }

    public IndentPrinter indent(int delta) {
        indent += delta;
        return this;
    }

    public IndentPrinter indent() {
        return indent(4);
    }

    public IndentPrinter dedent() {
        return dedent(4);
    }

    public IndentPrinter dedent(int delta) {
        indent -= delta;
        return this;
    }

    public IndentPrinter print(String string) {
        sb.append(string);
        return this;
    }

    public IndentPrinter printEncoded(String string) {
        FastEncoder.encode(string, sb);
        return this;
    }

    public IndentPrinter print(char c) {
        sb.append(c);
        return this;
    }

    public IndentPrinter println() {
        for (int i = 0; i < indent; i++) print(' ');
        if (sb.length() > 0) print('\n');
        return this;
    }

    public IndentPrinter println(String s) {
        for (int i = 0; i < indent; i++) print(' ');
        print(s);
        print('\n');
        return this;
    }

    public String getString() {
        return sb.toString();
    }

    public IndentPrinter clear() {
        sb.setLength(0);
        return this;
    }
}

