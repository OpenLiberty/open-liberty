/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ws;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.ws.WebserviceDescription;
import com.ibm.ws.javaee.dd.ws.Webservices;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.common.DescriptionGroup;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:element name="webservices"
 type="javaee:webservicesType">
 <xsd:key name="webservice-description-name-key">
 <xsd:selector xpath="javaee:webservice-description"/>
 <xsd:field xpath="javaee:webservice-description-name"/>
 </xsd:key>
 </xsd:element>

 <xsd:complexType name="webservicesType">
 <xsd:sequence>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="webservice-description"
 type="javaee:webservice-descriptionType"
 minOccurs="1"
 maxOccurs="unbounded">
 <xsd:key name="port-component-name-key">
 <xsd:selector xpath="javaee:port-component"/>
 <xsd:field xpath="javaee:port-component-name"/>
 </xsd:key>
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="version"
 type="javaee:dewey-versionType"
 fixed="1.3"
 use="required">
 </xsd:attribute>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */
public class WebservicesType extends DescriptionGroup implements Webservices,
                DDParser.RootParsable {
    public WebservicesType(String path) {
        this.path = path;
    }

    @Override
    public String getDeploymentDescriptorPath() {
        return path;
    }

    @Override
    public Object getComponentForId(String id) {
        return idMap.getComponentForId(id);
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return idMap.getIdForComponent(ddComponent);
    }

    @Override
    public String getVersion() {
        return version.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public List<WebserviceDescription> getWebServiceDescriptions() {
        if (webservice_descriptions != null) {
            return webservice_descriptions.getList();
        } else {
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (super.handleAttribute(parser, nsURI, localName, index))
            return true;

        if ("id".equals(localName)) {
            id = parser.getAttributeValue(index);
            return true;
        }

        if ("version".equals(localName)) {
            this.version = parser.parseTokenAttributeValue(index);
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }

        if ("webservice-description".equals(localName)) {
            WebserviceDescriptionType wsdes = new WebserviceDescriptionType();
            parser.parse(wsdes);
            addWebServiceDescription(wsdes);
            return true;
        }

        return false;
    }

    private void addWebServiceDescription(WebserviceDescriptionType wsdes) {
        if (this.webservice_descriptions == null) {
            this.webservice_descriptions = new WebserviceDescriptionType.ListType();
        }
        this.webservice_descriptions.add(wsdes);
    }

    /** {@inheritDoc} */
    @Override
    public void finish(DDParser parser) throws ParseException {
        this.idMap = parser.idMap;
        super.finish(parser);
        if (version == null) {
            throw new ParseException(parser.requiredAttributeMissing("version"));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }

    @Override
    protected String toTracingSafeString() {
        return "webservices";
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIdAllowed() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void describe(Diagnostics diag) {

        super.describe(diag);
        diag.describe("version", version);
        diag.describeIfSet("webservice-description", this.webservice_descriptions);
    }

    // key webservice-description-name-key
    Map<XSDTokenType, WebserviceDescriptionType> webserviceDescriptionNameToWebserviceDescriptionMap;

    final String path;
    // Component ID map
    DDParser.ComponentIDMap idMap;
    // Attributes
    TokenType version;
    String id;
    WebserviceDescriptionType.ListType webservice_descriptions;

}
