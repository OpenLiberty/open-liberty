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

import javax.xml.namespace.QName;

import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

public class QNameType extends AnySimpleType implements com.ibm.ws.javaee.dd.common.QName {

    @Override
    public String getNamespaceURI() {
        return value.getNamespaceURI();
    }

    @Override
    public String getLocalPart() {
        return value.getLocalPart();
    }

    public static QNameType wrap(DDParser parser, String wrapped) throws ParseException {
        return new QNameType(parser, wrapped);
    }

    // content
    private QName value;

    public QNameType() {
        super(Whitespace.collapse);
    }

    public QNameType(DDParser parser, String lexical) throws ParseException {
        super(Whitespace.collapse, parser, lexical);
    }

    @Override
    protected void setValueFromLexical(DDParser parser, String lexical) {
        // We may not be able to set the value without access to the in-scope namespaces,
        // so we defer attempting to do so until resolve is called
    }

    public void resolve(DDParser parser) {
        String lexical = getLexicalValue();
        int colonOffset = lexical.indexOf(':');
        if (colonOffset == -1) {
            value = new QName(lexical);
        } else {
            String localPart = lexical.substring(colonOffset + 1);
            String prefix = lexical.substring(0, colonOffset);
            String uri = parser.getNamespaceURI(prefix);
            value = new QName(uri, localPart, prefix);
        }
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        if (value != null) {
            if (value.getNamespaceURI() != null) {
                diag.append("{" + value.getNamespaceURI() + "}");
            }
            diag.append("\"" + value.getLocalPart() + "\"");
        } else {
            diag.append("null");
        }
    }
}
