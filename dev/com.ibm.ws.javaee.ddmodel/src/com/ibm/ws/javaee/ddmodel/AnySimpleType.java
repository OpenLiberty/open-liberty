/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableElement;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableList;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

public abstract class AnySimpleType implements ParsableElement {
    public static class ListType extends ParsableList<AnySimpleType> {
        @Override
        public AnySimpleType newInstance(DDParser parser) {
            return new StringType();
        }

        public List<String> getList() {
            List<String> stringList = new ArrayList<String>();
            for (AnySimpleType ast : list) {
                stringList.add(ast.getLexicalValue());
            }
            return stringList;
        }
    }

    public enum Whitespace {
        preserve,
        replace,
        collapse
    }

    @Trivial
    public String getLexicalValue() {
        return lexical;
    }

    @Trivial
    public static boolean isSet(AnySimpleType value) {
        return value != null && !value.isNil();
    }

    // content
    private final Whitespace wsfacet;
    private final boolean untrimmed;
    private boolean nilled;
    private String lexical;
    private String valueFromAttributeName;

    @Trivial
    protected AnySimpleType(Whitespace wsfacet) {
        this(wsfacet, false);
    }

    @Trivial
    protected AnySimpleType(Whitespace wsfacet, boolean untrimmed) {
        this.wsfacet = wsfacet;
        this.untrimmed = untrimmed;
    }

    @Trivial
    protected AnySimpleType(Whitespace wsfacet, DDParser parser, String lexical) throws ParseException {
        this(wsfacet);
        this.lexical = normalizeWhitespace(lexical);
        setValueFromLexical(parser, lexical);
    }

    protected abstract void setValueFromLexical(DDParser parser, String lexical) throws ParseException;

    @Trivial
    public void obtainValueFromAttribute(String attrName) {
        this.valueFromAttributeName = attrName;
    }

    @Trivial
    @Override
    public final void setNil(boolean nilled) {
        this.nilled = nilled;
    }

    @Trivial
    @Override
    public final boolean isNil() {
        return nilled;
    }

    @Trivial
    @Override
    public boolean isIdAllowed() {
        return false;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (valueFromAttributeName != null && valueFromAttributeName.equals(localName)) {
            this.lexical = normalizeWhitespace(parser.getAttributeValue(index));
            setValueFromLexical(parser, lexical);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleContent(DDParser parser) throws ParseException {
        if (valueFromAttributeName != null) {
            return false;
        }
        parser.appendTextToContent();
        return true;
    }

    @Trivial
    @Override
    public boolean handleChild(DDParser parser, String localName) {
        return false;
    }

    @Override
    public void finish(DDParser parser) throws ParseException {
        if (!isNil() && valueFromAttributeName == null) {
            lexical = normalizeWhitespace(parser.getContentString(untrimmed));
            setValueFromLexical(parser, lexical);
        }
    }

    @Trivial
    @Override
    public void describe(DDParser.Diagnostics diag) {
        if (isNil()) {
            diag.append("nilled");
        } else if (lexical != null) {
            diag.append("\"" + lexical + "\"");
        }
    }

    @Trivial
    protected String toTracingSafeString() {
        return super.toString();
    }

    @Trivial
    @Override
    public final String toString() {
        return toTracingSafeString();
    }

    @Trivial
    private String normalizeWhitespace(String initialValue) {
        switch (wsfacet) {
            case preserve:
                return initialValue;
            case replace:
                return replaceWhitespace(initialValue);
            case collapse:
                // Code to collapse whitespace that was formerly here has been moved to
                // MetatypeUtils. This may need to be moved to a more generic "StringUtils"
                // class that both can use, but we'll need to discuss whether we want to expose
                // that as SPI or not
                return MetatypeUtils.evaluateToken(initialValue);
        }
        return null;
    }

    /**
     * Replaces all occurrences of 0x9, 0xA and 0xD with 0x20.
     */
    @Trivial
    private static String replaceWhitespace(String value) {
        final int length = value.length();
        for (int i = 0; i < length; ++i) {
            char c = value.charAt(i);
            if (c < 0x20 && (c == 0x9 || c == 0xA || c == 0xD)) {
                return replace0(value, i, length);
            }
        }
        return value;
    }

    @Trivial
    private static String replace0(String value, int i, int length) {
        final StringBuilder sb = new StringBuilder(length);
        if (i > 0) {
            sb.append(value, 0, i);
        }
        sb.append(' ');
        while (++i < length) {
            char c = value.charAt(i);
            if (c < 0x20 && (c == 0x9 || c == 0xA || c == 0xD)) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
