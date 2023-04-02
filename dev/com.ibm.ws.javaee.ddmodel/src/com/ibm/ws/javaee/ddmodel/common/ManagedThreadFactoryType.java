/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import java.util.Collections;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

// <xsd:complexType name="managed-thread-factoryType">
// <xsd:sequence>
//   <xsd:element name="description" type="jakartaee:descriptionType" minOccurs="0">
//   <xsd:element name="name" type="jakartaee:jndi-nameType"/>
//   <xsd:element name="context-service-ref" type="jakartaee:jndi-nameType" minOccurs="0" maxOccurs="1"/>
//   <xsd:element name="priority" type="jakartaee:priorityType" minOccurs="0" maxOccurs="1"/>
//   <xsd:element name="property" type="jakartaee:propertyType" minOccurs="0" maxOccurs="unbounded"/>
// </xsd:sequence>
// <xsd:attribute name="id" type="xsd:ID"/>
// </xsd:complexType>

public class ManagedThreadFactoryType extends JNDIEnvironmentRefType implements ManagedThreadFactory {
    public static class ListType extends ParsableListImplements<ManagedThreadFactoryType, ManagedThreadFactory> {
        @Override
        public ManagedThreadFactoryType newInstance(DDParser parser) {
            return new ManagedThreadFactoryType();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Description> getDescriptions() {
        if ( descriptions != null ) {
            return (List<Description>) descriptions;
        } else {
            return Collections.emptyList();
        }
    }
    
    @Override
    public String getContextServiceRef() {
        return contextServiceRef == null ? null : contextServiceRef.getValue();
    }

    @Override
    public int getPriority() {
        return priority != null ? priority.getIntValue() : 0;
    }

    @Override
    public boolean isSetPriority() {
        return AnySimpleType.isSet(priority);
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

    private List<? extends Description> descriptions;
    private JNDINameType contextServiceRef;    
    private XSDIntegerType priority;
    private PropertyType.ListType properties;
    
    public ManagedThreadFactoryType() {
        super("name");
    }

    @Trivial
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
            descriptions = Collections.singletonList(description);
            return true;
        }
        

        if ("context-service-ref".equals(localName)) {
            if (contextServiceRef == null) {
                contextServiceRef = new JNDINameType();
            }
            parser.parse(contextServiceRef);
            return true;
        }

        if ("priority".equals(localName)) {
            XSDIntegerType priority = new XSDIntegerType();
            parser.parse(priority);
            this.priority = priority;
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
        if ( descriptions != null ) {
            diag.describe( "description", (DescriptionType) (descriptions.get(0)) );
        }
        super.describeHead(diag);
    }

    @Override
    public void describeBody(DDParser.Diagnostics diag) {
        super.describeBody(diag);
        diag.describe("context-service-ref", contextServiceRef);        
        diag.describeIfSet("priority", priority);
    }
    
    @Override
    public void describeTail(DDParser.Diagnostics diag) {
        super.describeTail(diag);
        diag.describeIfSet("property", properties);
    }
}
