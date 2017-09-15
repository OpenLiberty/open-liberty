/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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

import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="persistence-context-refType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="persistence-context-ref-name"
 type="javaee:jndi-nameType">
 </xsd:element>
 <xsd:element name="persistence-unit-name"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="persistence-context-type"
 type="javaee:persistence-context-typeType"
 minOccurs="0"/>
 <xsd:element name="persistence-context-synchronization"
 type="javaee:persistence-context-synchronizationType"
 minOccurs="0"/>
 <xsd:element name="persistence-property"
 type="javaee:propertyType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:group ref="javaee:resourceBaseGroup"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class PersistenceContextRefType extends PersistenceRefType implements PersistenceContextRef {

    public static class ListType extends ParsableListImplements<PersistenceContextRefType, PersistenceContextRef> {
        @Override
        public PersistenceContextRefType newInstance(DDParser parser) {
            return new PersistenceContextRefType();
        }
    }

    @Override
    public int getTypeValue() {
        if (persistence_context_type != null) {
            switch (persistence_context_type.value) {
                case Transaction:
                    return TYPE_TRANSACTION;
                case Extended:
                    return TYPE_EXTENDED;
            }
        }
        return TYPE_UNSPECIFIED;
    }

    @Override
    public int getSynchronizationValue() {
        return persistence_context_synchronization != null ? persistence_context_synchronization.value.value : PersistenceContextRef.SYNCHRONIZATION_UNSPECIFIED;
    }

    @Override
    public List<Property> getProperties() {
        if (persistence_property != null) {
            return persistence_property.getList();
        } else {
            return Collections.emptyList();
        }
    }

    private PersistenceContextTypeType persistence_context_type;
    private PersistenceContextSynchronizationType persistence_context_synchronization;
    private PropertyType.ListType persistence_property;

    public PersistenceContextRefType() {
        super("persistence-context-ref-name");
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("persistence-context-type".equals(localName)) {
            PersistenceContextTypeType persistence_context_type = new PersistenceContextTypeType();
            parser.parse(persistence_context_type);
            this.persistence_context_type = persistence_context_type;
            return true;
        }
        if (parser.eePlatformVersion >= 70 && "persistence-context-synchronization".equals(localName)) {
            PersistenceContextSynchronizationType persistence_context_synchronization = new PersistenceContextSynchronizationType();
            parser.parse(persistence_context_synchronization);
            this.persistence_context_synchronization = persistence_context_synchronization;
            return true;
        }
        if ("persistence-property".equals(localName)) {
            PropertyType persistence_property = new PropertyType();
            parser.parse(persistence_property);
            addPersistenceProperty(persistence_property);
            return true;
        }
        return false;
    }

    private void addPersistenceProperty(PropertyType persistence_property) {
        if (this.persistence_property == null) {
            this.persistence_property = new PropertyType.ListType();
        }
        this.persistence_property.add(persistence_property);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describeIfSet("persistence-context-type", persistence_context_type);
        diag.describeIfSet("persistence-context-synchronization", persistence_context_synchronization);
        diag.describeIfSet("persistence-property", persistence_property);
    }

    /*
     * <xsd:complexType name="persistence-context-typeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:xsdTokenType">
     * <xsd:enumeration value="Transaction"/>
     * <xsd:enumeration value="Extended"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static enum PersistenceContextTypeEnum {
        // lexical value must be (Transaction|Extended)
        Transaction,
        Extended;
    }

    static class PersistenceContextTypeType extends XSDTokenType {
        // content
        PersistenceContextTypeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, PersistenceContextTypeEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    static enum PersistenceContextSynchronizationEnum {
        Synchronized(PersistenceContextRef.SYNCHRONIZATION_SYNCHRONIZED),
        Unsynchronized(PersistenceContextRef.SYNCHRONIZATION_UNSYNCHRONIZED);

        final int value;

        PersistenceContextSynchronizationEnum(int value) {
            this.value = value;
        }
    }

    static class PersistenceContextSynchronizationType extends XSDTokenType {
        // content
        PersistenceContextSynchronizationEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, PersistenceContextSynchronizationEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }
}
