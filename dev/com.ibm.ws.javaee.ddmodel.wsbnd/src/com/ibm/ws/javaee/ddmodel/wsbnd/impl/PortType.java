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
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.Map;

import javax.xml.namespace.QName;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.wsbnd.Port;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.StringUtils;

/*
 <xsd:complexType name="portType">
 <xsd:sequence>
 <xsd:element name="properties" minOccurs="0" type="ws:propertiesType" />
 </xsd:sequence>
 <xsd:attribute name="name" type="xsd:string" use="required" />
 <xsd:attribute name="namespace" type="xsd:string" />
 <xsd:attribute name="address" type="xsd:string" />
 <xsd:attribute name="username" type="xsd:string" />
 <xsd:attribute name="password" type="xsd:string" />
 <xsd:attribute name="ssl-ref" type="xsd:string" />
 <xsd:attribute name="key-alias" type="xsd:string" />
 </xsd:complexType>
 */
public class PortType extends DDParser.ElementContentParsable implements Port {
    private StringType namespace;
    private StringType name;
    private StringType address;
    private StringType sslRef;
    private StringType keyAlias;
    private StringType userName;
    private ProtectedString password;
    private PropertiesType properties;

    @Override
    public QName getPortQName() {
        return StringUtils.buildQName(getNamespace(), getName());
    }

    @Override
    public String getNamespace() {
        return namespace != null ? namespace.getValue() : null;
    }

    @Override
    public String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public String getAddress() {
        return address != null ? address.getValue() : null;
    }

    @Override
    public String getUserName() {
        return userName != null ? userName.getValue() : null;
    }

    @Override
    public ProtectedString getPassword() {
        return password;
    }

    @Override
    public String getSSLRef() {
        return sslRef != null ? sslRef.getValue() : null;
    }

    @Override
    public String getKeyAlias() {
        return keyAlias != null ? keyAlias.getValue() : null;
    }

    @Override
    public Map<String, String> getProperties() {
        return null != properties ? properties.getAttributes() : null;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName)
                    throws ParseException {
        if (PROPERTIES_ELEMENT_NAME.equals(localName)) {
            properties = new PropertiesType();
            parser.parse(properties);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        boolean result = false;

        if (nsURI != null) {
            return result;
        }

        if (NAMESPACE_ATTRIBUTE_NAME.equals(localName)) {
            namespace = parser.parseStringAttributeValue(index);
            result = true;
        } else if (NAME_ATTRIBUTE_NAME.equals(localName)) {
            name = parser.parseStringAttributeValue(index);
            result = true;
        } else if (ADDRESS_ATTRIBUTE_NAME.equals(localName)) {
            address = parser.parseStringAttributeValue(index);
            result = true;
        } else if (USER_NAME_ATTRIBUTE_NAME.equals(localName)) {
            userName = parser.parseStringAttributeValue(index);
            result = true;
        } else if (PASSWORD_ATTRIBUTE_NAME.equals(localName)) {
            password = new ProtectedString(parser.getAttributeValue(index).toCharArray());
            result = true;
        } else if (SSL_REF_ATTRIBUTE_NAME.equals(localName)) {
            sslRef = parser.parseStringAttributeValue(index);
            return true;
        } else if (ALIAS_ATTRIBUTE_NAME.equals(localName)) {
            keyAlias = parser.parseStringAttributeValue(index);
            return true;
        }

        return result;
    }

    @Override
    public void describe(Diagnostics diag) {
        diag.describe(NAMESPACE_ATTRIBUTE_NAME, namespace);
        diag.describe(NAME_ATTRIBUTE_NAME, name);
        diag.describe(ADDRESS_ATTRIBUTE_NAME, address);
        diag.describe(USER_NAME_ATTRIBUTE_NAME, userName);
        diag.describe(SSL_REF_ATTRIBUTE_NAME, sslRef);
        diag.describe(ALIAS_ATTRIBUTE_NAME, keyAlias);
        diag.describe(PROPERTIES_ELEMENT_NAME, properties);
    }

    //ONLY used by WsClientBinding
    public static PortType createPortType(StringType namespace, StringType name, StringType address) {
        PortType endpointType = new PortType();
        endpointType.namespace = namespace;
        endpointType.name = name;
        endpointType.address = address;
        return endpointType;

    }
}
