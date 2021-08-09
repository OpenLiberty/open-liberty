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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableList;
import com.ibm.ws.javaee.ddmodel.TokenType;

/*
 <xsd:complexType name="string">
 <xsd:simpleContent>
 <xsd:extension base="xsd:token">
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:extension>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class XSDTokenType extends TokenType {

    public static class ListType extends ParsableList<XSDTokenType> {
        @Override
        public XSDTokenType newInstance(DDParser parser) {
            return new XSDTokenType();
        }

        public List<String> getList() {
            List<String> stringList = new ArrayList<String>();
            for (XSDTokenType token : list) {
                stringList.add(token.getValue());
            }
            return stringList;
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }
}
