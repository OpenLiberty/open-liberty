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
 <xsd:complexType name="ejb-refType">
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
 <xsd:element name="home"
 type="javaee:homeType"
 minOccurs="0"/>
 <xsd:element name="remote"
 type="javaee:remoteType"
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

/*
 <xsd:complexType name="remoteType">
 <xsd:simpleContent>
 <xsd:restriction base="javaee:fully-qualified-classType"/>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class EJBRefType extends ResourceGroup implements EJBRef {

    public static class ListType extends ParsableListImplements<EJBRefType, EJBRef> {
        @Override
        public EJBRefType newInstance(DDParser parser) {
            return new EJBRefType();
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
        return KIND_REMOTE;
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
        return home != null ? home.getValue() : null;
    }

    @Override
    public String getInterface() {
        return remote != null ? remote.getValue() : null;
    }

    @Override
    public String getLink() {
        return ejb_link != null ? ejb_link.getValue() : null;
    }

    // elements
    DescriptionType.ListType description;
    EJBRefTypeType ejb_ref_type;
    XSDTokenType home;
    XSDTokenType remote;
    XSDTokenType ejb_link;

    // ResourceGroup fields appear here in sequence

    public EJBRefType() {
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
        if ("home".equals(localName)) {
            XSDTokenType home = new XSDTokenType();
            parser.parse(home);
            this.home = home;
            return true;
        }
        if ("remote".equals(localName)) {
            XSDTokenType remote = new XSDTokenType();
            parser.parse(remote);
            this.remote = remote;
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
        diag.describeIfSet("home", home);
        diag.describeIfSet("remote", remote);
        diag.describeIfSet("ejb-link", ejb_link);
        super.describe(diag);
    }
}
