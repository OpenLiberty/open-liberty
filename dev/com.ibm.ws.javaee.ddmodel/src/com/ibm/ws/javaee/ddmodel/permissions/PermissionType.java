/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.permissions;

import com.ibm.ws.javaee.dd.permissions.Permission;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;

/**
 *
 */
/*
 * <xsd:complexType>
 * <xsd:sequence>
 * <xsd:element name="class-name"
 * type="javaee:fully-qualified-classType"/>
 * <xsd:element name="name"
 * type="javaee:string"
 * minOccurs="0"/>
 * <xsd:element name="actions"
 * type="javaee:string"
 * minOccurs="0"/>
 * </xsd:sequence>
 * </xsd:complextType>
 */
class PermissionType extends DDParser.ElementContentParsable implements Permission {

    public static class ListType extends ParsableListImplements<PermissionType, Permission> {
        @Override
        public PermissionType newInstance(DDParser parser) {
            return new PermissionType();
        }
    }

    StringType className = null;
    StringType name = null;
    StringType actions = null;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.ddmodel.DDParser.ParsableElement#handleChild(com.ibm.ws.javaee.ddmodel.DDParser, java.lang.String)
     */
    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("class-name".equals(localName)) {
            StringType cN = new StringType();
            parser.parse(cN);
            this.className = cN;
            return true;
        }
        if ("name".equals(localName)) {
            StringType n = new StringType();
            parser.parse(n);
            this.name = n;
            return true;
        }
        if ("actions".equals(localName)) {
            StringType a = new StringType();
            parser.parse(a);
            this.actions = a;
            return true;
        }
        return false;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.ddmodel.DDParser.Parsable#describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics)
     */
    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("class-name", className);
        diag.describeIfSet("name", name);
        diag.describeIfSet("actions", actions);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.permissions.Permission#getClassName()
     */
    @Override
    public String getClassName() {
        return className.getValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.permissions.Permission#getName()
     */
    @Override
    public String getName() {
        // null check added for name
        if (name != null) {
            return name.getValue();
        }
        else {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.javaee.dd.permissions.Permission#getActions()
     */
    @Override
    public String getActions() {
        if (actions != null) {
            return actions.getValue();
        } else {
            return null;
        }
    }

}
