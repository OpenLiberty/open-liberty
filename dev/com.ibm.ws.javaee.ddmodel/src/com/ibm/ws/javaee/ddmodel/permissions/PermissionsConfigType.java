/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.permissions;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.permissions.PermissionsConfig;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.TokenType;

/*
 * <xsd:element name="permissions">
 * <xsd:complexType>
 * <xsd:sequence>
 * <xsd:element name="permission"
 * type="javaee:permissionType"
 * maxOccurs="unbounded"
 * minOccurs="0"/>
 * </xsd:element>
 * </xsd:sequence>
 * </xsd:element>
 */

public class PermissionsConfigType extends DDParser.ElementContentParsable implements PermissionsConfig, DDParser.RootParsable {

    final String path;

    PermissionType.ListType permissions;

    public PermissionsConfigType(String path) {
        this.path = path;
    }

    public void parsePermission(DDParser parser) throws ParseException {
        PermissionType permission = new PermissionType();
        parser.parse(permission);
        addPermission(permission);
    }

    private void addPermission(PermissionType permission) {
        if (this.permissions == null) {
            this.permissions = new PermissionType.ListType();
        }
        this.permissions.add(permission);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("permission".equals(localName)) {
            parsePermission(parser);
            return true;
        }
        return false;
    }

    // attributes
    TokenType version;

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (nsURI == null) {
            if (parser.version >= 70 && "version".equals(localName)) {
                version = parser.parseTokenAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public void describe(Diagnostics diag) {
        diag.describeIfSet("permissions", permissions);
    }
  
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List getPermissions() {
        if (this.permissions != null) {
            return this.permissions.getList();
        } else
            return Collections.emptyList();
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return null;
    }

    @Override
    public Object getComponentForId(String id) {
        return null;
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return null;
    }

    @Override
    public void describe(StringBuilder sb) {
        // EMPTY
    }
}
