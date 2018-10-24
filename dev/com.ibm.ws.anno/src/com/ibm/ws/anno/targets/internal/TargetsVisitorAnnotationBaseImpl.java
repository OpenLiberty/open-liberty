/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.internal;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * <p>Core detail text generating visitor for annotations. Used for annotation
 * and array visit steps.</p>
 * 
 * <p>The visitor is intended to be reused. That will eventually result in a
 * tree of child visitors. However, the depth of the tree is limited to the
 * maximum depth of annotation values, which, outside of pathological test cases,
 * is small.</p>
 */
public abstract class TargetsVisitorAnnotationBaseImpl extends AnnotationVisitor {
    /**
     * <p>Standard constructor. Construct a non-delegating root detail visitor.</p>
     */
    public TargetsVisitorAnnotationBaseImpl(TargetsVisitorClassImpl parentVisitor) {
        super(Opcodes.ASM7);

        this.parentVisitor = parentVisitor;

        this.detailText = new StringBuilder();
        this.encoder = new StringBuilder();
    }

    /**
     * <p>Standard constructor. Construct a non-delegating nested detail visitor.</p>
     */
    public TargetsVisitorAnnotationBaseImpl(StringBuilder parentDetailText, StringBuilder parentEncoder) {
        super(Opcodes.ASM7);

        this.parentVisitor = null;

        this.detailText = parentDetailText;
        this.encoder = parentEncoder;
    }

    //

    protected final TargetsVisitorClassImpl parentVisitor;

    @Trivial
    public TargetsVisitorClassImpl getParentVisitor() {
        return parentVisitor;
    }

    // Nested here means nested beneath another detail visitor.

    @Trivial
    public boolean isNested() {
        return (parentVisitor == null);
    }

    //

    protected TargetsVisitorAnnotationImpl childAnnotationVisitor;

    @Override
    public TargetsVisitorAnnotationImpl visitAnnotation(String name, String desc) {
        if (childAnnotationVisitor == null) {
            childAnnotationVisitor = new TargetsVisitorAnnotationImpl(getDetailText(), getEncoder());
        }

        return childAnnotationVisitor;
    }

    protected TargetsVisitorAnnotationArrayImpl childArrayVisitor;

    @Override
    public TargetsVisitorAnnotationArrayImpl visitArray(String name) {
        if (childArrayVisitor == null) {
            childArrayVisitor = new TargetsVisitorAnnotationArrayImpl(getDetailText(), getEncoder());
        }

        return childArrayVisitor;
    }

    //

    /** <p>Detail text builder.</p> */
    protected final StringBuilder detailText;

    protected void appendName(String name) {
        detailText.append(name);
    }

    protected void appendValue(String value) {
        detailText.append(DOUBLE_QUOTE);
        detailText.append(encode(value));
        detailText.append(DOUBLE_QUOTE);
    }

    protected void append(char value) {
        detailText.append(value);
    }

    protected StringBuilder getDetailText() {
        return detailText;
    }

    public String retrieveDetailText() {
        String text = detailText.toString();
        detailText.setLength(0);
        return text;
    }

    //

    public static final char OPENING_BRACE = '{';
    public static final char CLOSING_BRACE = '}';
    public static final char ASSIGNMENT = '=';
    public static final char OPENING_BRACKET = '[';
    public static final char CLOSING_BRACKET = ']';
    public static final char COMMA = ',';

    public static final char DOUBLE_QUOTE = '"';
    public static final char FORWARD_SLASH = '/';

    protected final StringBuilder encoder;

    protected StringBuilder getEncoder() {
        return encoder;
    }

    public String encode(String value) {
        boolean doEscape = ((value.indexOf(DOUBLE_QUOTE) != -1) ||
                            (value.indexOf(FORWARD_SLASH) != -1));
        if (!doEscape) {
            return value;
        }

        int valueLen = value.length();
        for (int charNo = 0; !doEscape && charNo < valueLen; charNo++) {
            char nextChar = value.charAt(charNo);
            if ((nextChar == DOUBLE_QUOTE) || (nextChar == FORWARD_SLASH)) {
                encoder.append(FORWARD_SLASH);
            }
            encoder.append(nextChar);
        }

        String encodedValue = encoder.toString();

        encoder.setLength(0);

        return encodedValue;
    }

    //

    // For safety, do a reset before visit calls, 
    // in case the last call failed with an exception.

    protected void reset() {
        // Only reset the detail text and encoder of the root
        // detail visitor.  All of the child detail visitors
        // share the root detail text and encoder.

        if (parentVisitor != null) {
            detailText.setLength(0);
            encoder.setLength(0);
        }
    }
}
