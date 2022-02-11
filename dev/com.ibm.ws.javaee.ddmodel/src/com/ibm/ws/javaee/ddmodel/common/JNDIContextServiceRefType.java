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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.JNDIContextServiceRef;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

// <xsd:complexType name="managed-thread-factoryType">
// <xsd:sequence>
//   <xsd:element name="description" type="jakartaee:descriptionType" minOccurs="0">
//   <xsd:element name="name" type="jakartaee:jndi-nameType"/>
//   <xsd:element name="context-service-ref" type="jakartaee:jndi-nameType" minOccurs="0" maxOccurs="1">
//   <xsd:element name="property" type="jakartaee:propertyType" minOccurs="0" maxOccurs="unbounded"/>
//   &etc
// </xsd:sequence>
// <xsd:attribute name="id" type="xsd:ID"/>
// </xsd:complexType>

public class JNDIContextServiceRefType
    extends JNDIEnvironmentRefType
    implements JNDIContextServiceRef {

    @Trivial
    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public Description getDescription() {
        return description;
    }

    @Override
    public String getContextServiceRef() {
        return contextServiceRef.getValue();
    }

    @Override
    public List<Property> getProperties() {
        if (properties != null) {
            return properties.getList();
        } else {
            return Collections.emptyList();
        }
    }

    //

    private DescriptionType description;
    private JNDINameType contextServiceRef;
    private PropertyType.ListType properties;

    public JNDIContextServiceRefType(String element_local_name) {
        super(element_local_name);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) { // Handle 'name'
            return true;
        }

        if ("description".equals(localName)) {
            if (description == null ) {
                description = new DescriptionType();
            }
            parser.parse(description);
            return true;
        }

        if ("context-service-ref".equals(localName)) {
            if (contextServiceRef == null) {
                contextServiceRef = new JNDINameType();
            }
            parser.parse(contextServiceRef);
            return true;
        }

        if ("property".equals(localName)) {
            PropertyType property = new PropertyType();
            parser.parse(property);
            if (properties == null) {
                properties = new PropertyType.ListType();
            }
            properties.add(property);
            return true;
        }

        return false;
    }

    //
    
    @Override
    public void describeHead(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        super.describeHead(diag);
    }

    @Override
    public void describeBody(DDParser.Diagnostics diag) {
        super.describeBody(diag);
        diag.describe("context-service-ref", contextServiceRef);
    }

    @Override
    public void describeTail(DDParser.Diagnostics diag) {
        super.describeTail(diag);
        diag.describeIfSet("property", properties);
    }
}
