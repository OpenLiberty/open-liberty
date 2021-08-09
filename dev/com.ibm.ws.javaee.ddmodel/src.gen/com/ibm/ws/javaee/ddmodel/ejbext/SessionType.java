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

public class SessionType extends com.ibm.ws.javaee.ddmodel.ejbext.EnterpriseBeanType implements com.ibm.ws.javaee.dd.ejbext.Session {
    public SessionType() {
        this(false);
    }

    public SessionType(boolean xmi) {
        super(xmi);
    }

    com.ibm.ws.javaee.ddmodel.ejbext.TimeOutType time_out;

    @Override
    public com.ibm.ws.javaee.dd.ejbext.TimeOut getTimeOut() {
        return time_out;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (xmi && "timeout".equals(localName)) {
                if (this.time_out == null) {
                    this.time_out = new com.ibm.ws.javaee.ddmodel.ejbext.TimeOutType(true);
                }
                this.time_out.value = parser.parseIntegerAttributeValue(index);
                return true;
            }
        }
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("type".equals(localName)) {
                String type = parser.getAttributeValue(index);
                if (type.endsWith(":SessionExtension") && "ejbext.xmi".equals(parser.getNamespaceURI(type.substring(0, type.length() - ":SessionExtension".length())))) {
                    // Allowed but ignored.
                    return true;
                }
            }
        }
        return super.handleAttribute(parser, nsURI, localName, index);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (!xmi && "time-out".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbext.TimeOutType time_out = new com.ibm.ws.javaee.ddmodel.ejbext.TimeOutType();
            parser.parse(time_out);
            this.time_out = time_out;
            return true;
        }
        return super.handleChild(parser, localName);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            if (time_out != null) {
                time_out.describe(diag);
            }
        } else {
            diag.describeIfSet("time-out", time_out);
        }
    }
}
