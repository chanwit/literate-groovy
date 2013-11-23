package org.codehaus.groovy.control;

import org.codehaus.groovy.control.messages.SimpleMessage;
import org.pegdown.Printer;
import org.pegdown.ast.*;

import java.math.BigInteger;
import java.security.MessageDigest;

public class NewSpockVisitor implements org.pegdown.ast.Visitor {

    private RootNode root = null;
    private String singleFeature = "";
    private Printer out = new Printer();
    private int currentLevel = 0;
    boolean printPackage = false;
    private String superClass = "spock.lang.Specification";
    private String mixin = "sut.sater.Sikuli";
    private String line = "false";

    private State state = State.START;


    private enum State {
        START,
        SPEC,
        SPEC_DESCRIPTION,
        SPEC_BLOCK,
        FEATURE,
        FEATURE_BLOCK,
        FEATURE_BLOCK_CODE,
        FEATURE_BLOCK_TABLE
    }

    private LineMap lineMap;
    private SourceUnit su;
    private ErrorCollector errorCollector;

    public NewSpockVisitor() {
        this(null);
        su = null;
        errorCollector = null;
    }

    public NewSpockVisitor(LineMap lineMap, SourceUnit sourceUnit) {
        this(lineMap);
        this.su = sourceUnit;
        this.errorCollector = su.getErrorCollector();
    }

    public NewSpockVisitor(LineMap lineMap) {
        final String property = System.getProperty("single.feature");
        if(property != null) {
            this.singleFeature = property.trim();
        }

        final String lineProperty = System.getProperty("line");
        if(lineProperty != null) {
            this.line = lineProperty.trim();
        }

        this.lineMap = lineMap;

        this.su = null;
        this.errorCollector = null;
    }

    private String md5sum(String s) {

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(s.getBytes("UTF8"),0,s.getBytes("UTF8").length);
            String result = new BigInteger(1, md5.digest()).toString(16).toLowerCase();
            // DEBUG: System.out.println("md5: " + s + "\n>>> " + result);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public String toSource(RootNode astRoot) {
        root = astRoot;
        astRoot.accept(this);
        return out.getString();
    }

    protected void visitChildren(SuperNode node) {
        for (Node child : node.getChildren()) {
            // DEBUG:
            // System.out.println("State : " + state + ", " + child);
            child.accept(this);
        }
    }


    public void visit(RootNode node) {
        if(printPackage == false) {
            out.print("package toplevel");
            out.println();
            out.println();
            printPackage = true;
        }

        visitChildren(node);

        if(is(State.FEATURE_BLOCK_CODE)) {
            change(State.FEATURE_BLOCK_CODE, State.FEATURE_BLOCK);
        } else if(is(State.FEATURE_BLOCK_TABLE)) {
            change(State.FEATURE_BLOCK_TABLE, State.FEATURE_BLOCK);
        }

        if(is(State.FEATURE_BLOCK)) {
            out.indent(-4);
            out.println();
            out.print("}");
            change(State.FEATURE_BLOCK, State.FEATURE);
        }

        if(is(State.FEATURE)) {
            out.indent(-4);
            out.println();
            out.print("}");
            change(State.FEATURE, State.SPEC);
        }

    }

    private String methodName = "";
    private String blockComment = "";
    private String rowContent = "";

    private void printMethod(HeaderNode node) {
        methodName = "";
        visitChildren(node);
    }

    private void change(State from, State to) {
        if(state == from) {
            state = to;
        } else {
            // throw new RuntimeException("Expecting '" + from + "' state");
            errorCollector.addFatalError(new SimpleMessage("Expecting state = " + state, su));
        }
    }

    private Boolean is(State s) {
        return state == s;
    }


    public void visit(HeaderNode node) {
        currentLevel = node.getLevel();
        if (is(State.START) && currentLevel == 1) {
            change(State.START, State.SPEC);
            out.print("// ");
            visitChildren(node);
        } else if (is(State.SPEC) && currentLevel == 2) {
            printClass(node);
            change(State.SPEC, State.FEATURE);
            out.indent(4);
            printMethod(node);
        } else if (is(State.FEATURE_BLOCK_CODE) && currentLevel == 2) {
            out.indent(-4);
            out.println();
            out.print("}");
            out.println();
            change(State.FEATURE_BLOCK_CODE, State.FEATURE);
            printMethod(node);
        } else if(is(State.START) && currentLevel == 2) {
            mixin = "sut.sater.Sikuli";
            printClass();
            out.println();
            change(State.START, State.FEATURE);
            out.indent(4);
            printMethod(node);
        } else {
            // throw new RuntimeException("NYI");
            errorCollector.addFatalError(new SimpleMessage("NYI: state = " + state, su));
        }
    }


    public void visit(TextNode node) {
        final String text = node.getText();
        if(is(State.SPEC) || is(State.SPEC_DESCRIPTION)) {
            out.print(text);
        } else
        if(is(State.FEATURE)) {
            methodName = methodName + text;
        } else
        if(is(State.FEATURE_BLOCK)) {
            String lowerText = text.toLowerCase();
            String blockVerb = Words.getBlockVerb(lowerText);
            if(blockVerb != null) {
                out.print(blockVerb + " ");
            }
            blockComment += text;
        } else
        if(is(State.FEATURE_BLOCK_TABLE)) {
            //out.print(text + "| ");
            rowContent += text;
        }
        else {
            // throw new RuntimeException("NYI");
            errorCollector.addFatalError(new SimpleMessage("NYI: state = " + state, su));
        }
    }


    public void visit(TableNode node) {
        if(is(State.FEATURE_BLOCK)) {
            change(State.FEATURE_BLOCK, State.FEATURE_BLOCK_TABLE);
            out.print('"');
            out.print(blockComment);
            out.print('"');
            visitChildren(node);
        }
    }


    public void visit(SpecialTextNode node) {
        final String allowedChars = "._";
        final char ch = node.getText().charAt(0);
        boolean allowed = allowedChars.indexOf(ch) != -1;

        if(is(State.FEATURE) && allowed)
            methodName += ch;
        else if(is(State.FEATURE_BLOCK) && allowed)
            blockComment += ch;
        else if(allowed)
            out.print(ch);
    }


    public void visit(CodeNode node) {
        out.print(node.getText());
    }


    public void visit(ParaNode node) {
        //
        // if it's a Header-less document
        //
        if(is(State.START)) {
            mixin = "sut.sater.Sikuli";
            printClass();
            out.println();
            out.indent(4);
            out.println();
            out.print("def \"Unnamed\"() {");

            change(State.START, State.FEATURE_BLOCK);
            blockComment = "";
            out.indent(4);
            out.println();
            visitChildren(node);
        } else

        //
        // if Header Level is 1
        // all paragraph is just a description
        //
        if(is(State.SPEC)) {
            change(State.SPEC, State.SPEC_DESCRIPTION);
            out.println();
            out.println();
            out.print("/* ");
            visitChildren(node);
            out.print(" */");
            change(State.SPEC_DESCRIPTION, State.SPEC);
        } else

        //
        // if Header Level is 2
        // all paragraph will be a Spock block
        //
        if(is(State.FEATURE)) {
            if(md5sum(methodName).equals(singleFeature)) {
                out.println();
                out.print("@spock.lang.IgnoreRest\n");
            }
            out.println();
            out.print("def \"");
            out.print(methodName);
            out.print("\"() {");

            change(State.FEATURE, State.FEATURE_BLOCK);
            blockComment = "";
            out.indent(4);
            out.println();
            visitChildren(node);
        }

        if(is(State.FEATURE_BLOCK_CODE)) {
            change(State.FEATURE_BLOCK_CODE, State.FEATURE_BLOCK);
            blockComment = "";
            out.println();
            out.println();
            visitChildren(node);
        }
    }

    private void printClass(HeaderNode node) {
        out.println();
        printClass();
        out.println();
    }

    private void printClass() {
        out.print("@Mixin(" + mixin + ")");
        out.println();
        out.print("class UntitledSpec extends " + superClass + " {");
    }



    public void visit(VerbatimNode node) {
        if(is(State.SPEC)) {
            change(State.SPEC, State.SPEC_BLOCK);
            String[] lines = node.getText().split("\n");
            for(String line: lines) {
                String[] command = line.split(" ");
                if(command.length == 2) {
                    if(command[0].startsWith("ใช้") || command[0].startsWith("use")) {
                        if(command[1].equals("Sikuli")) {
                            mixin = "sut.sater.Sikuli";
                        } else
                        if(command[1].equals("Selenium")) {
                            mixin = "sut.sater.Selenium";
                        } else
                        errorCollector.addFatalError(new SimpleMessage("Expected 'Sikuli' or 'Selenium', but found: '" + command[1] +"'", su));
                        // throw new RuntimeException("Expected 'Sikuli' or 'Selenium', but found: '" + command[1] +"'");
                    } else
                    errorCollector.addFatalError(new SimpleMessage("Expected keyword 'use' (or equivalent), but found: '" + command[0] + "'", su));
                    // throw new RuntimeException("Expected keyword 'use' (or equivalent), but found: '" + command[0] + "'");
                } else
                errorCollector.addFatalError(new SimpleMessage("Expected keyword 'use' but found: '" + line + "'", su));
                // throw new RuntimeException("Expected keyword 'use' but found: '" + line + "'");
            }
            change(State.SPEC_BLOCK, State.SPEC);
        }

        if(is(State.FEATURE_BLOCK)) {
            change(State.FEATURE_BLOCK, State.FEATURE_BLOCK_CODE);
            out.print('"');
            out.print(blockComment);
            out.print('"');
        }

        if(is(State.FEATURE_BLOCK_CODE)) {
            String[] lines = node.getText().split("\n");
            int startingLine = 0;
            if(lineMap != null) {
                startingLine = lineMap.lineOf(node.getStartIndex()+1);
            }
            for(int no=0; no < lines.length; no++) {
                if(lineMap != null) {
                    if(line.equals("true")) {
                        out.println();
                        out.print("line("); out.print("" + (startingLine + no)); out.print(")");
                    }
                }
                out.println();
                out.print(lines[no]);
            }
        }
    }


    public void visit(SuperNode node) {
        visitChildren(node);
    }


    public void visit(SimpleNode node) {
        switch(node.getType()) {
            case Linebreak :
                out.println();
                break;

            default:
                errorCollector.addFatalError(new SimpleMessage("NYI", su));
        }
    }


    public void visit(AbbreviationNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(AutoLinkNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(BlockQuoteNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(BulletListNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(DefinitionListNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(DefinitionNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(DefinitionTermNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(EmphNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(ExpImageNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(ExpLinkNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(HtmlBlockNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(InlineHtmlNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(ListItemNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(MailLinkNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(OrderedListNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(QuotedNode node) {
        if(is(State.FEATURE_BLOCK_TABLE)) {
            rowContent = rowContent+'"';
            visitChildren(node);
            rowContent = rowContent+'"';
        } else {
            errorCollector.addFatalError(new SimpleMessage("NYI", su));
        }
    }


    public void visit(ReferenceNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(RefImageNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(RefLinkNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(StrongNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(TableBodyNode node) {
        visitChildren(node);
    }


    public void visit(TableCaptionNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(TableCellNode node) {
        rowContent += "| ";
        visitChildren(node);
    }


    public void visit(TableColumnNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(TableHeaderNode node) {
        visitChildren(node);
    }


    public void visit(TableRowNode node) {
        out.println();
        rowContent = "";
        visitChildren(node);
        // strip the first "| " out of rowContent
        out.print(rowContent.substring(2, rowContent.length()));
    }


    public void visit(WikiLinkNode node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }


    public void visit(Node node) {
        errorCollector.addFatalError(new SimpleMessage("NYI", su));
    }

}
