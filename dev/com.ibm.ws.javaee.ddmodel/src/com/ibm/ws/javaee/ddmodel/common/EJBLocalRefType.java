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
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="ejb-local-refType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="ejb-ref-name"
 type="javaee:ejb-ref-nameType"/>
 <xsd:element name="ejb-ref-type"
 type="javaee:ejb-ref-typeType"
 minOccurs="0"/>
 <xsd:element name="local-home"
 type="javaee:local-homeType"
 minOccurs="0"/>
 <xsd:element name="local"
 type="javaee:localType"
 minOccurs="0"/>
 <xsd:element name="ejb-link"
 type="javaee:ejb-linkType"
 minOccurs="0"/>
 <xsd:group ref="javaee:resourceGroup"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class EJBLocalRefType extends ResourceGroup implements EJBRef {

    public static class ListType extends ParsableListImplements<EJBLocalRefType, EJBRef> {
        @Override
        public EJBLocalRefType newInstance(DDParser parser) {
            return new EJBLocalRefType();
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
    public int getKindValue() {
        return KIND_LOCAL;
    }

    @Override
    public int getTypeValue() {
        if (ejb_ref_type != null) {
            switch (ejb_ref_type.value) {
                case Entity:
                    return TYPE_ENTITY;
                case Session:
                    return TYPE_SESSION;
            }
        }
        return TYPE_UNSPECIFIED;
    }

    @Override
    public String getHome() {
        return local_home != null ? local_home.getValue() : null;
    }

    @Override
    public String getInterface() {
        return local != null ? local.getValue() : null;
    }

    @Override
    public String getLink() {
        return ejb_link != null ? ejb_link.getValue() : null;
    }

    // elements
    DescriptionType.ListType description;
    EJBRefTypeType ejb_ref_type;
    XSDTokenType local_home;
    XSDTokenType local;
    XSDTokenType ejb_link;

    // ResourceGroup fields appear here in sequence

    public EJBLocalRefType() {
        super("ejb-ref-name");
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
        if ("ejb-ref-type".equals(localName)) {
            EJBRefTypeType ejb_ref_type = new EJBRefTypeType();
            parser.parse(ejb_ref_type);
            this.ejb_ref_type = ejb_ref_type;
            return true;
        }
        if ("local-home".equals(localName)) {
            XSDTokenType local_home = new XSDTokenType();
            parser.parse(local_home);
            this.local_home = local_home;
            return true;
        }
        if ("local".equals(localName)) {
            XSDTokenType local = new XSDTokenType();
            parser.parse(local);
            this.local = local;
            return true;
        }
        if ("ejb-link".equals(localName)) {
            XSDTokenType ejb_link = new XSDTokenType();
            parser.parse(ejb_link);
            this.ejb_link = ejb_link;
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
        diag.describeIfSet("ejb-ref-type", ejb_ref_type);
        diag.describeIfSet("local-home", local_home);
        diag.describeIfSet("local", local);
        diag.describeIfSet("ejb-link", ejb_link);
        super.describe(diag);
    }
}
