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

import com.ibm.ws.javaee.dd.common.SecurityRoleRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="security-role-refType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="role-name"
 type="javaee:role-nameType">
 </xsd:element>
 <xsd:element name="role-link"
 type="javaee:role-nameType"
 minOccurs="0">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class SecurityRoleRefType extends DescribableType implements SecurityRoleRef {

    public static class ListType extends ParsableListImplements<SecurityRoleRefType, SecurityRoleRef> {
        @Override
        public SecurityRoleRefType newInstance(DDParser parser) {
            return new SecurityRoleRefType();
        }
    }

    @Override
    public String getName() {
        return role_name.getValue();
    }

    @Override
    public String getLink() {
        return role_link != null ? role_link.getValue() : null;
    }

    // elements
    XSDTokenType role_name = new XSDTokenType();
    XSDTokenType role_link;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("role-name".equals(localName)) {
            parser.parse(role_name);
            return true;
        }
        if ("role-link".equals(localName)) {
            XSDTokenType role_link = new XSDTokenType();
            parser.parse(role_link);
            this.role_link = role_link;
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describe("role-name", role_name);
        diag.describeIfSet("role-link", role_link);
    }
}
