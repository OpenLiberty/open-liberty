/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

// <xsd:complexType name="managed-executorType">
// <xsd:sequence>
//   <xsd:element name="description" type="jakartaee:descriptionType" minOccurs="0"/>
//   <xsd:element name="name" type="jakartaee:jndi-nameType"/>
//   <xsd:element name="context-service-ref" type="jakartaee:jndi-nameType" minOccurs="0" maxOccurs="1"/>
//   <xsd:element name="max-async" type="jakartaee:xsdPositiveIntegerType" minOccurs="0" maxOccurs="1"/>
//   <xsd:element name="hung-task-threshold" type="jakartaee:xsdPositiveIntegerType" minOccurs="0" maxOccurs="1"/>
//   <xsd:element name="virtual" type="jakartaee:true-falseType" minOccurs="0" maxOccurs="1"/>
//   <xsd:element name="qualifier" type="jakartaee:fully-qualified-classType" minOccurs="0" maxOccurs="unbounded"/>
//   <xsd:element name="property" type="jakartaee:propertyType" minOccurs="0" maxOccurs="unbounded"/>
// </xsd:sequence>
// <xsd:attribute name="id" type="xsd:ID"/>
// </xsd:complexType>

public class ManagedScheduledExecutorType extends JNDIEnvironmentRefType implements ManagedScheduledExecutor {
    public static class ListType extends ParsableListImplements<ManagedScheduledExecutorType, ManagedScheduledExecutor> {
        @Override
        public ManagedScheduledExecutorType newInstance(DDParser parser) {
            return new ManagedScheduledExecutorType();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Description> getDescriptions() {
        if (descriptions != null) {
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
    public boolean isSetMaxAsync() {
        return AnySimpleType.isSet(maxAsync);
    }

    @Override
    public int getMaxAsync() {
        return maxAsync != null ? maxAsync.getIntValue() : 0;
    }

    @Override
    public boolean isSetHungTaskThreshold() {
        return AnySimpleType.isSet(hungTaskThreshold);
    }

    @Override
    public long getHungTaskThreshold() {
        return hungTaskThreshold != null ? hungTaskThreshold.getIntValue() : 0;
    }

    @Override
    public boolean isSetVirtual() {
        return AnySimpleType.isSet(virtual);
    }

    @Override
    public boolean isVirtual() {
        return virtual != null ? virtual.getBooleanValue() : false;
    }

    @Override
    public String[] getQualifiers() {
        if (qualifiers != null) {
            return qualifiers.getArray();
        } else {
            return XSDTokenType.ListType.EMPTY_ARRAY;
        }
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
    private XSDIntegerType maxAsync;
    private XSDIntegerType hungTaskThreshold;
    private XSDBooleanType virtual;
    private XSDTokenType.ListType qualifiers;
    private PropertyType.ListType properties;

    public ManagedScheduledExecutorType() {
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

        if ("hung-task-threshold".equals(localName)) {
            XSDIntegerType hungTaskThreshold = new XSDIntegerType();
            parser.parse(hungTaskThreshold);
            this.hungTaskThreshold = hungTaskThreshold;
            return true;
        }

        if ("max-async".equals(localName)) {
            XSDIntegerType maxAsync = new XSDIntegerType();
            parser.parse(maxAsync);
            this.maxAsync = maxAsync;
            return true;
        }

        if ("virtual".equals(localName)) {
            XSDBooleanType virtual = new XSDBooleanType();
            parser.parse(virtual);
            this.virtual = virtual;
            return true;
        }

        if ("qualifier".equals(localName)) {
            XSDTokenType unchanged_element = new XSDTokenType();
            parser.parse(unchanged_element);
            if (qualifiers == null) {
                qualifiers = new XSDTokenType.ListType();
            }
            qualifiers.add(unchanged_element);
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
        if (descriptions != null) {
            diag.describe("description", (DescriptionType) (descriptions.get(0)));
        }
        super.describeHead(diag);
    }

    @Override
    public void describeBody(DDParser.Diagnostics diag) {
        super.describeBody(diag);
        diag.describe("context-service-ref", contextServiceRef);
        diag.describeIfSet("hung-task-threshold", hungTaskThreshold);
        diag.describeIfSet("max-async", maxAsync);
        diag.describeIfSet("virtual", virtual);
        diag.describeIfSet("qualifier", qualifiers);
    }

    @Override
    public void describeTail(DDParser.Diagnostics diag) {
        super.describeTail(diag);
        diag.describeIfSet("property", properties);
    }

}
