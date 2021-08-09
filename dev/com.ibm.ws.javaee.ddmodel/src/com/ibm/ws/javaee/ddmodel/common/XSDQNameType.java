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
package com.ibm.ws.javaee.ddmodel.common;

import com.ibm.ws.javaee.dd.common.QName;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.QNameType;

/*
 <xsd:complexType name="xsdQNameType">
 <xsd:simpleContent>
 <xsd:extension base="xsd:QName">
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:extension>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class XSDQNameType extends QNameType {

    public static class ListType extends ParsableListImplements<XSDQNameType, QName> {
        @Override
        public XSDQNameType newInstance(DDParser parser) {
            return new XSDQNameType();
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }
}
