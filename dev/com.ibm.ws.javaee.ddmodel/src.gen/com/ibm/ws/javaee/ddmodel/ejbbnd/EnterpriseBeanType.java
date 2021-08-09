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

public class EnterpriseBeanType extends com.ibm.ws.javaee.ddmodel.commonbnd.RefBindingsGroupType implements com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean {
    public static class CmpConnectionFactoryXMIIgnoredType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable {
        public CmpConnectionFactoryXMIIgnoredType() {
            this(false);
        }

        public CmpConnectionFactoryXMIIgnoredType(boolean xmi) {
            this.xmi = xmi;
        }

        protected final boolean xmi;

        @Override
        public boolean isIdAllowed() {
            return xmi;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
            return true;
        }

        @Override
        public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        }
    }

    public EnterpriseBeanType() {
        this(false);
    }

    public EnterpriseBeanType(boolean xmi) {
        super(xmi);
    }

    com.ibm.ws.javaee.ddmodel.StringType name;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType enterpriseBean;
    CmpConnectionFactoryXMIIgnoredType cmpConnectionFactory;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        super.finish(parser);
        if (name == null) {
            throw new DDParser.ParseException(parser.requiredAttributeMissing("name"));
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
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
                this.name = parser.parseString(referent.getName());
            }
            return true;
        }
        if (xmi && "cmpConnectionFactory".equals(localName)) {
            CmpConnectionFactoryXMIIgnoredType cmpConnectionFactory = new CmpConnectionFactoryXMIIgnoredType(xmi);
            parser.parse(cmpConnectionFactory);
            this.cmpConnectionFactory = cmpConnectionFactory;
            return true;
        }
        return super.handleChild(parser, localName);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            diag.describeIfSet("enterpriseBean", enterpriseBean);
        } else {
            diag.describeIfSet("name", name);
        }
        diag.describeIfSet("cmpConnectionFactory", cmpConnectionFactory);
    }
}
