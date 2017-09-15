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
package com.ibm.ws.javaee.ddmodel.ejbext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class MessageDrivenType extends com.ibm.ws.javaee.ddmodel.ejbext.EnterpriseBeanType implements com.ibm.ws.javaee.dd.ejbext.MessageDriven {
    public MessageDrivenType() {
        this(false);
    }

    public MessageDrivenType(boolean xmi) {
        super(xmi);
    }


    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("type".equals(localName)) {
                String type = parser.getAttributeValue(index);
                if (type.endsWith(":MessageDrivenExtension") && "ejbext.xmi".equals(parser.getNamespaceURI(type.substring(0, type.length() - ":MessageDrivenExtension".length())))) {
                    // Allowed but ignored.
                    return true;
                }
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
    }
}
