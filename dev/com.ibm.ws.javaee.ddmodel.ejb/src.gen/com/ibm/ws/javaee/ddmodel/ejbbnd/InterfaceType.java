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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class InterfaceType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.ejbbnd.Interface {
    com.ibm.ws.javaee.ddmodel.StringType binding_name;
    com.ibm.ws.javaee.ddmodel.StringType class_;

    @Override
    public java.lang.String getBindingName() {
        return binding_name != null ? binding_name.getValue() : null;
    }

    @Override
    public java.lang.String getClassName() {
        return class_ != null ? class_.getValue() : null;
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        if (binding_name == null) {
            throw new DDParser.ParseException(parser.requiredAttributeMissing("binding-name"));
        }
        if (class_ == null) {
            throw new DDParser.ParseException(parser.requiredAttributeMissing("class"));
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ("binding-name".equals(localName)) {
                this.binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if ("class".equals(localName)) {
                this.class_ = parser.parseStringAttributeValue(index);
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
        diag.describeIfSet("binding-name", binding_name);
        diag.describeIfSet("class", class_);
    }
}
