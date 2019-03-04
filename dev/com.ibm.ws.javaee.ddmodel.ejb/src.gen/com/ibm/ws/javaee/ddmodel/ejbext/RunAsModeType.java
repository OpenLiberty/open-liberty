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

public class RunAsModeType extends com.ibm.ws.javaee.ddmodel.ejbext.RunAsModeBaseType implements com.ibm.ws.javaee.dd.ejbext.RunAsMode {
    public RunAsModeType() {
        this(false);
    }

    public RunAsModeType(boolean xmi) {
        super(xmi);
    }

    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbext.ExtendedMethodType, com.ibm.ws.javaee.dd.ejbext.ExtendedMethod> method;

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.ejbext.ExtendedMethod> getMethods() {
        if (method != null) {
            return method.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        super.finish(parser);
        if (method == null) {
            throw new DDParser.ParseException(parser.missingElement("method"));
        }
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        return super.handleAttribute(parser, nsURI, localName, index);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if ((xmi ? "methodElements" : "method").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbext.ExtendedMethodType method = new com.ibm.ws.javaee.ddmodel.ejbext.ExtendedMethodType(xmi);
            parser.parse(method);
            this.addMethod(method);
            return true;
        }
        return super.handleChild(parser, localName);
    }

    void addMethod(com.ibm.ws.javaee.ddmodel.ejbext.ExtendedMethodType method) {
        if (this.method == null) {
            this.method = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbext.ExtendedMethodType, com.ibm.ws.javaee.dd.ejbext.ExtendedMethod>();
        }
        this.method.add(method);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet(xmi ? "methodElements" : "method", method);
    }
}
