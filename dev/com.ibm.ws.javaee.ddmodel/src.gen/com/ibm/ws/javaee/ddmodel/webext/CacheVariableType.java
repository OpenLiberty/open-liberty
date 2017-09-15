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
package com.ibm.ws.javaee.ddmodel.webext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class CacheVariableType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.webext.CacheVariable {
    com.ibm.ws.javaee.dd.webext.CacheVariable.TypeEnum type;
    com.ibm.ws.javaee.ddmodel.StringType identifier;
    com.ibm.ws.javaee.ddmodel.StringType method;
    com.ibm.ws.javaee.ddmodel.BooleanType required;
    com.ibm.ws.javaee.ddmodel.StringType data_id;
    com.ibm.ws.javaee.ddmodel.StringType invalidate;

    @Override
    public boolean isSetType() {
        return type != null;
    }

    @Override
    public com.ibm.ws.javaee.dd.webext.CacheVariable.TypeEnum getType() {
        return type;
    }

    @Override
    public java.lang.String getIdentifier() {
        return identifier != null ? identifier.getValue() : null;
    }

    @Override
    public java.lang.String getMethod() {
        return method != null ? method.getValue() : null;
    }

    @Override
    public boolean isSetRequired() {
        return required != null;
    }

    @Override
    public boolean isRequired() {
        return required != null ? required.getBooleanValue() : false;
    }

    @Override
    public java.lang.String getDataId() {
        return data_id != null ? data_id.getValue() : null;
    }

    @Override
    public java.lang.String getInvalidate() {
        return invalidate != null ? invalidate.getValue() : null;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ("type".equals(localName)) {
                this.type = parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.webext.CacheVariable.TypeEnum.class);
                return true;
            }
            if ("identifier".equals(localName)) {
                this.identifier = parser.parseStringAttributeValue(index);
                return true;
            }
            if ("method".equals(localName)) {
                this.method = parser.parseStringAttributeValue(index);
                return true;
            }
            if ("required".equals(localName)) {
                this.required = parser.parseBooleanAttributeValue(index);
                return true;
            }
            if ("data-id".equals(localName)) {
                this.data_id = parser.parseStringAttributeValue(index);
                return true;
            }
            if ("invalidate".equals(localName)) {
                this.invalidate = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        return false;
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeEnumIfSet("type", type);
        diag.describeIfSet("identifier", identifier);
        diag.describeIfSet("method", method);
        diag.describeIfSet("required", required);
        diag.describeIfSet("data-id", data_id);
        diag.describeIfSet("invalidate", invalidate);
    }
}
