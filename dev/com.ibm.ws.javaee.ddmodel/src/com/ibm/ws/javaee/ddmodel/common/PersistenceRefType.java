/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.PersistenceRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="persistence-refType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="persistence-unit-name"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:group ref="javaee:resourceBaseGroup"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class PersistenceRefType extends ResourceBaseGroup implements PersistenceRef {

    @Override
    public List<Description> getDescriptions() {
        if (description != null) {
            return description.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getPersistenceUnitName() {
        return persistence_unit_name != null ? persistence_unit_name.getValue() : null;
    }

    // elements
    DescriptionType.ListType description;
    XSDTokenType persistence_unit_name;

    // ResourceBaseGroup fields appear here in sequence

    protected PersistenceRefType(String element_local_name) {
        super(element_local_name);
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
        if ("persistence-unit-name".equals(localName)) {
            XSDTokenType persistence_unit_name = new XSDTokenType();
            parser.parse(persistence_unit_name);
            this.persistence_unit_name = persistence_unit_name;
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

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        diag.describeIfSet("persistence-unit-name", persistence_unit_name);
        super.describe(diag);
    }
}
