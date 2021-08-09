/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.commonext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class MethodType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonext.Method {
    public MethodType() {
        this(false);
    }

    public MethodType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    com.ibm.ws.javaee.ddmodel.StringType params;
    com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum type;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public java.lang.String getParams() {
        return params != null ? params.getValue() : null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum getType() {
        return type;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            // "name" is the same for XML and XMI.
            if ("name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
            if ((xmi ? "parms" : "params").equals(localName)) {
                this.params = parser.parseStringAttributeValue(index);
                return true;
            }
            // "type" is the same for XML and XMI.
            if ("type".equals(localName)) {
                this.type = xmi ? parseXMIMethodTypeEnumAttributeValue(parser, index) : parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum.class);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        return false;
    }

    private static com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum parseXMIMethodTypeEnumAttributeValue(DDParser parser, int index) throws DDParser.ParseException {
        String value = parser.getAttributeValue(index);
        if ("Unspecified".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum.UNSPECIFIED;
        }
        if ("Remote".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum.REMOTE;
        }
        if ("Home".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum.HOME;
        }
        if ("Local".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum.LOCAL;
        }
        if ("LocalHome".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum.LOCAL_HOME;
        }
        if ("ServiceEndpoint".equals(value)) {
            return com.ibm.ws.javaee.dd.commonext.Method.MethodTypeEnum.SERVICE_ENDPOINT;
        }
        throw new DDParser.ParseException(parser.invalidEnumValue(value, "Unspecified", "Remote", "Home", "Local", "LocalHome", "ServiceEndpoint"));
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("name", name);
        diag.describeIfSet(xmi ? "parms" : "params", params);
        diag.describeEnumIfSet("type", type);
    }
}
