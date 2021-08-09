/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;

/*
 <xsd:complexType name="xsdStringType">
 <xsd:simpleContent>
 <xsd:extension base="xsd:string">
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:extension>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class XSDStringType extends StringType {
    public static XSDStringType wrap(DDParser parser, String wrapped) throws ParseException {
        return new XSDStringType(parser, wrapped);
    }

    public XSDStringType() {}

    public XSDStringType(boolean untrimmed) {
        super(untrimmed);
    }

    protected XSDStringType(DDParser parser, String lexical) throws ParseException {
        super(Whitespace.preserve, parser, lexical);
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }
}
