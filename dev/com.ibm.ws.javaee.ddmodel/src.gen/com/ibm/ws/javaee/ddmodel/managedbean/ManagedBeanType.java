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
package com.ibm.ws.javaee.ddmodel.managedbean;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class ManagedBeanType extends com.ibm.ws.javaee.ddmodel.commonbnd.RefBindingsGroupType implements com.ibm.ws.javaee.dd.managedbean.ManagedBean {
    public ManagedBeanType() {
        this(false);
    }

    public ManagedBeanType(boolean xmi) {
        super(xmi);
    }

    com.ibm.ws.javaee.ddmodel.StringType class_;

    @Override
    public java.lang.String getClazz() {
        return class_ != null ? class_.getValue() : null;
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        super.finish(parser);
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
            if (!xmi && "class".equals(localName)) {
                this.class_ = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return super.handleAttribute(parser, nsURI, localName, index);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        return super.handleChild(parser, localName);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("class", class_);
    }
}
