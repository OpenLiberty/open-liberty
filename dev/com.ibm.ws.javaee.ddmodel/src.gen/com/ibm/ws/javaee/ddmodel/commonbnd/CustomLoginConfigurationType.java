/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.commonbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class CustomLoginConfigurationType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration {
    public CustomLoginConfigurationType() {
        this(false);
    }

    public CustomLoginConfigurationType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.PropertyType, com.ibm.ws.javaee.dd.commonbnd.Property> property;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.Property> getProperties() {
        if (property != null) {
            return property.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ((xmi ? "loginConfigurationName" : "name").equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if ((xmi ? "properties" : "property").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.PropertyType property = new com.ibm.ws.javaee.ddmodel.commonbnd.PropertyType(xmi);
            parser.parse(property);
            this.addProperty(property);
            return true;
        }
        return false;
    }

    void addProperty(com.ibm.ws.javaee.ddmodel.commonbnd.PropertyType property) {
        if (this.property == null) {
            this.property = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.commonbnd.PropertyType, com.ibm.ws.javaee.dd.commonbnd.Property>();
        }
        this.property.add(property);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet(xmi ? "loginConfigurationName" : "name", name);
        diag.describeIfSet(xmi ? "properties" : "property", property);
    }
}
