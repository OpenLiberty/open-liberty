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
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="env-entryType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="env-entry-name"
 type="javaee:jndi-nameType">
 </xsd:element>
 <xsd:element name="env-entry-type"
 type="javaee:env-entry-type-valuesType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="env-entry-value"
 type="javaee:xsdStringType"
 minOccurs="0">
 </xsd:element>
 <xsd:group ref="javaee:resourceGroup"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class EnvEntryType extends ResourceGroup implements EnvEntry {

    public static class ListType extends ParsableListImplements<EnvEntryType, EnvEntry> {
        @Override
        public EnvEntryType newInstance(DDParser parser) {
            return new EnvEntryType();
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
    public String getTypeName() {
        return env_entry_type != null ? env_entry_type.getValue() : null;
    }

    @Override
    public String getValue() {
        return env_entry_value != null ? env_entry_value.getValue() : null;
    }

    // elements
    DescriptionType.ListType description;
    XSDTokenType env_entry_type;
    XSDStringType env_entry_value;

    // ResourceGroup fields appear here in sequence

    public EnvEntryType() {
        super("env-entry-name");
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
        if ("env-entry-type".equals(localName)) {
            XSDTokenType env_entry_type = new XSDTokenType();
            parser.parse(env_entry_type);
            this.env_entry_type = env_entry_type;
            return true;
        }
        if ("env-entry-value".equals(localName)) {
            XSDStringType env_entry_value = new XSDStringType(true);
            parser.parse(env_entry_value);
            this.env_entry_value = env_entry_value;
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
        diag.describeIfSet("env-entry-type", env_entry_type);
        diag.describeIfSet("env-entry-value", env_entry_value);
        super.describe(diag);
    }
}
