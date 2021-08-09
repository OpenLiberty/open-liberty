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
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="resource-env-refType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="resource-env-ref-name"
 type="javaee:jndi-nameType">
 </xsd:element>
 <xsd:element name="resource-env-ref-type"
 type="javaee:fully-qualified-classType"
 minOccurs="0">
 </xsd:element>
 <xsd:group ref="javaee:resourceGroup"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class ResourceEnvRefType extends ResourceGroup implements ResourceEnvRef {

    public static class ListType extends ParsableListImplements<ResourceEnvRefType, ResourceEnvRef> {
        @Override
        public ResourceEnvRefType newInstance(DDParser parser) {
            return new ResourceEnvRefType();
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
        return resource_env_ref_type != null ? resource_env_ref_type.getValue() : null;
    }

    // elements
    DescriptionType.ListType description;
    XSDTokenType resource_env_ref_type;

    // ResourceGroup fields appear here in sequence

    public ResourceEnvRefType() {
        super("resource-env-ref-name");
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
        if ("resource-env-ref-type".equals(localName)) {
            XSDTokenType resource_env_ref_type = new XSDTokenType();
            parser.parse(resource_env_ref_type);
            this.resource_env_ref_type = resource_env_ref_type;
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
        diag.describeIfSet("resource-env-ref-type", resource_env_ref_type);
        super.describe(diag);
    }
}
