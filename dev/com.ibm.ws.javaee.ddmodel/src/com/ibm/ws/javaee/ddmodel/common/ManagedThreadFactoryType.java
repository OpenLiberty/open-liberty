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

import com.ibm.ws.javaee.dd.common.ConnectionFactory;
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

public class ManagedThreadFactoryType extends JNDIContextServiceRefType implements ManagedThreadFactory {
    public static class ListType extends ParsableListImplements<ManagedThreadFactoryType, ManagedThreadFactory> {
        @Override
        public ManagedThreadFactoryType newInstance(DDParser parser) {
            return new ManagedThreadFactoryType();
        }
    }

    @Override
    public int getPriority() {
        return priority != null ? priority.getIntValue() : 0;
    }

    @Override
    public boolean isSetPriority() {
        return AnySimpleType.isSet(priority);
    }

    //

    private XSDIntegerType priority;

    public ManagedThreadFactoryType() {
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

        if ("priority".equals(localName)) {
            XSDIntegerType priority = new XSDIntegerType();
            parser.parse(priority);
            this.priority = priority;
            return true;
        }

        return false;
    }

    @Override
    public void describeBody(DDParser.Diagnostics diag) {
        super.describeBody(diag);
        diag.describeIfSet("priority", priority);
    }
}
