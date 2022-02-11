/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import com.ibm.ws.javaee.dd.common.ManagedExecutor;
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
//   <xsd:element name="property" type="jakartaee:propertyType" minOccurs="0" maxOccurs="unbounded"/>
// </xsd:sequence>
// <xsd:attribute name="id" type="xsd:ID"/>
// </xsd:complexType>

public class ManagedExecutorType extends JNDIContextServiceRefType implements ManagedExecutor {
    public static class ListType extends ParsableListImplements<ManagedExecutorType, ManagedExecutor> {
        @Override
        public ManagedExecutorType newInstance(DDParser parser) {
            return new ManagedExecutorType();
        }
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
    public int getHungTaskThreshold() {
        return hungTaskThreshold != null ? hungTaskThreshold.getIntValue() : 0;
    }
    
    //

    private XSDIntegerType maxAsync;
    private XSDIntegerType hungTaskThreshold;

    public ManagedExecutorType() {
        super("name");
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
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

        return false;
    }

    @Override
    public void describeBody(DDParser.Diagnostics diag) {
        super.describeBody(diag);
        diag.describeIfSet("hung-task-threshold", hungTaskThreshold);        
        diag.describeIfSet("max-async", maxAsync);
    }
}
