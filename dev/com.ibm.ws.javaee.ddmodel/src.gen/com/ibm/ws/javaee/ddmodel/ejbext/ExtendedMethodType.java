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

public class ExtendedMethodType extends com.ibm.ws.javaee.ddmodel.commonext.MethodType implements com.ibm.ws.javaee.dd.ejbext.ExtendedMethod {
    public ExtendedMethodType() {
        this(false);
    }

    public ExtendedMethodType(boolean xmi) {
        super(xmi);
    }

    com.ibm.ws.javaee.ddmodel.StringType ejb;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType enterpriseBean;

    @Override
    public java.lang.String getEJB() {
        return ejb != null ? ejb.getValue() : null;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "ejb".equals(localName)) {
                this.ejb = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return super.handleAttribute(parser, nsURI, localName, index);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "enterpriseBean".equals(localName)) {
            this.enterpriseBean = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("enterpriseBean", parser.crossComponentDocumentType);
            parser.parse(enterpriseBean);
            com.ibm.ws.javaee.dd.ejb.EnterpriseBean referent = this.enterpriseBean.resolveReferent(parser, com.ibm.ws.javaee.dd.ejb.EnterpriseBean.class);
            if (referent == null) {
                DDParser.unresolvedReference("enterpriseBean", this.enterpriseBean.getReferenceString());
            } else {
                this.ejb = parser.parseString(referent.getName());
            }
            return true;
        }
        return super.handleChild(parser, localName);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            diag.describeIfSet("enterpriseBean", enterpriseBean);
        } else {
            diag.describeIfSet("ejb", ejb);
        }
    }
}
