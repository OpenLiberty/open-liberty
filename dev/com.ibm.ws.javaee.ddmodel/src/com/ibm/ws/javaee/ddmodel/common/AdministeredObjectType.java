/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.AdministeredObject;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

public class AdministeredObjectType extends JNDIEnvironmentRefType implements AdministeredObject {
    public static class ListType extends ParsableListImplements<AdministeredObjectType, AdministeredObject> {
        @Override
        public AdministeredObjectType newInstance(DDParser parser) {
            return new AdministeredObjectType();
        }
    }

    @Override
    public List<Description> getDescriptions() {
        if (description != null) {
            return description.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getInterfaceNameValue() {
        return interface_name != null ? interface_name.getValue() : null;
    }

    @Override
    public String getClassNameValue() {
        return class_name != null ? class_name.getValue() : null;
    }

    @Override
    public String getResourceAdapter() {
        return resource_adapter != null ? resource_adapter.getValue() : null;
    }

    @Override
    public List<Property> getProperties() {
        if (property != null) {
            return property.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // elements
    private DescriptionType.ListType description;
    private XSDTokenType interface_name;
    private XSDTokenType class_name;
    private XSDTokenType resource_adapter;
    private PropertyType.ListType property;

    public AdministeredObjectType() {
        super("name");
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("description".equals(localName)) {
            DescriptionType description = new DescriptionType();
            parser.parse(description);
            addDescription(description);
            return true;
        }
        if ("interface-name".equals(localName)) {
            XSDTokenType interface_name = new XSDTokenType();
            parser.parse(interface_name);
            this.interface_name = interface_name;
            return true;
        }
        if ("class-name".equals(localName)) {
            XSDTokenType class_name = new XSDTokenType();
            parser.parse(class_name);
            this.class_name = class_name;
            return true;
        }
        if ("resource-adapter".equals(localName)) {
            XSDTokenType resource_adapter = new XSDTokenType();
            parser.parse(resource_adapter);
            this.resource_adapter = resource_adapter;
            return true;
        }
        if ("property".equals(localName)) {
            PropertyType property = new PropertyType();
            parser.parse(property);
            addProperty(property);
            return true;
        }
        return false;
    }

    private void addDescription(DescriptionType description) {
        if (this.description == null) {
            this.description = new DescriptionType.ListType();
        }
        this.description.add(description);
    }

    private void addProperty(PropertyType property) {
        if (this.property == null) {
            this.property = new PropertyType.ListType();
        }
        this.property.add(property);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        super.describe(diag);
        diag.describeIfSet("interface-name", interface_name);
        diag.describeIfSet("class-name", class_name);
        diag.describeIfSet("resource-adapter", resource_adapter);
        diag.describeIfSet("property", property);
    }
}
