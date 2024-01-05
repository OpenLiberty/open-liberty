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
import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;

// <xsd:complexType name="context-serviceType">
// <xsd:sequence>
//   <xsd:element name="description" type="jakartaee:descriptionType" minOccurs="0"/>
//   <xsd:element name="name" type="jakartaee:jndi-nameType"/>
//   <xsd:element name="cleared" type="jakartaee:string" minOccurs="0" maxOccurs="unbounded"/>
//   <xsd:element name="propagated" type="jakartaee:string" minOccurs="0" maxOccurs="unbounded"/>
//   <xsd:element name="unchanged" type="jakartaee:string" minOccurs="0" maxOccurs="unbounded"/>
//   <xsd:element name="qualifier" type="jakartaee:fully-qualified-classType" minOccurs="0" maxOccurs="unbounded"/>
//   <xsd:element name="property" type="jakartaee:propertyType" minOccurs="0" maxOccurs="unbounded"/>
// </xsd:sequence>
// <xsd:attribute name="id" type="xsd:ID"/>
// </xsd:complexType>

public class ContextServiceType extends JNDIEnvironmentRefType implements ContextService {
    public static class ListType extends ParsableListImplements<ContextServiceType, ContextService> {
        @Override
        public ContextServiceType newInstance(DDParser parser) {
            return new ContextServiceType();
        }
    }

    @Trivial
    @Override
    public boolean isIdAllowed() {
        return true;
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
    public String[] getCleared() {
        if (cleared == null) {
            // Per the xsd documentation, "Absent other configuration, cleared context defaults to Transaction."
            // However, we cannot default it here because we need to represent the state of unspecified
            // so that merging with annotations can be performed correctly.
            return StringType.ListType.getEmptyArray();
        } else {
            return cleared.getArray();
        }
    }

    @Override
    public String[] getPropagated() {
        if (propagated != null) {
            return propagated.getArray();
        } else {
            return StringType.ListType.getEmptyArray();
        }
    }

    @Override
    public String[] getUnchanged() {
        if (unchanged != null) {
            return unchanged.getArray();
        } else {
            return StringType.ListType.getEmptyArray();
        }
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
    private StringType.ListType cleared;
    private StringType.ListType propagated;
    private StringType.ListType unchanged;
    private XSDTokenType.ListType qualifiers;
    private PropertyType.ListType properties;

    public ContextServiceType() {
        super("name");
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

        if ("cleared".equals(localName)) {
            StringType cleared_element = new StringType();
            parser.parse(cleared_element);
            if (cleared == null) {
                cleared = new StringType.ListType();
            }
            cleared.add(cleared_element);
            return true;
        }

        if ("propagated".equals(localName)) {
            StringType propagated_element = new StringType();
            parser.parse(propagated_element);
            if (propagated == null) {
                propagated = new StringType.ListType();
            }
            propagated.add(propagated_element);
            return true;
        }

        if ("unchanged".equals(localName)) {
            StringType unchanged_element = new StringType();
            parser.parse(unchanged_element);
            if (unchanged == null) {
                unchanged = new StringType.ListType();
            }
            unchanged.add(unchanged_element);
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
        diag.describeIfSet("cleared", cleared);
        diag.describeIfSet("propagated", propagated);
        diag.describeIfSet("unchanged", unchanged);
        diag.describeIfSet("qualifier", qualifiers);
    }

    @Override
    public void describeTail(DDParser.Diagnostics diag) {
        super.describeTail(diag);
        diag.describeIfSet("property", properties);
    }

}
