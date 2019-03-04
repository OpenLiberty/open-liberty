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

public class ServletExtensionType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.webext.ServletExtension {
    public ServletExtensionType() {
        this(false);
    }

    public ServletExtensionType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType extendedServlet;
    com.ibm.ws.javaee.ddmodel.commonext.LocalTransactionType local_transaction;
    com.ibm.ws.javaee.ddmodel.commonext.GlobalTransactionType global_transaction;
    com.ibm.ws.javaee.ddmodel.webext.WebGlobalTransactionType web_global_transaction;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.MarkupLanguageType, com.ibm.ws.javaee.dd.webext.MarkupLanguage> markup_language;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.LocalTransaction getLocalTransaction() {
        return local_transaction;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonext.GlobalTransaction getGlobalTransaction() {
        return global_transaction;
    }

    @Override
    public com.ibm.ws.javaee.dd.webext.WebGlobalTransaction getWebGlobalTransaction() {
        return web_global_transaction;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.MarkupLanguage> getMarkupLanguages() {
        if (markup_language != null) {
            return markup_language.getList();
        }
        return java.util.Collections.emptyList();
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
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "extendedServlet".equals(localName)) {
            this.extendedServlet = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("extendedServlet", parser.crossComponentDocumentType);
            parser.parse(extendedServlet);
            com.ibm.ws.javaee.dd.web.common.Servlet referent = this.extendedServlet.resolveReferent(parser, com.ibm.ws.javaee.dd.web.common.Servlet.class);
            if (referent == null) {
                DDParser.unresolvedReference("extendedServlet", this.extendedServlet.getReferenceString());
            } else {
                this.name = parser.parseString(referent.getServletName());
            }
            return true;
        }
        if ((xmi ? "localTransaction" : "local-transaction").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonext.LocalTransactionType local_transaction = new com.ibm.ws.javaee.ddmodel.commonext.LocalTransactionType(xmi);
            parser.parse(local_transaction);
            this.local_transaction = local_transaction;
            return true;
        }
        if ((xmi ? "globalTransaction" : "global-transaction").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonext.GlobalTransactionType global_transaction = new com.ibm.ws.javaee.ddmodel.commonext.GlobalTransactionType(xmi);
            parser.parse(global_transaction);
            this.global_transaction = global_transaction;
            return true;
        }
        if ((xmi ? "webGlobalTransaction" : "web-global-transaction").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.WebGlobalTransactionType web_global_transaction = new com.ibm.ws.javaee.ddmodel.webext.WebGlobalTransactionType(xmi);
            parser.parse(web_global_transaction);
            this.web_global_transaction = web_global_transaction;
            return true;
        }
        if ((xmi ? "markupLanguages" : "markup-language").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.MarkupLanguageType markup_language = new com.ibm.ws.javaee.ddmodel.webext.MarkupLanguageType(xmi);
            parser.parse(markup_language);
            this.addMarkupLanguage(markup_language);
            return true;
        }
        return false;
    }

    void addMarkupLanguage(com.ibm.ws.javaee.ddmodel.webext.MarkupLanguageType markup_language) {
        if (this.markup_language == null) {
            this.markup_language = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.MarkupLanguageType, com.ibm.ws.javaee.dd.webext.MarkupLanguage>();
        }
        this.markup_language.add(markup_language);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            diag.describeIfSet("extendedServlet", extendedServlet);
        } else {
            diag.describeIfSet("name", name);
        }
        diag.describeIfSet(xmi ? "localTransaction" : "local-transaction", local_transaction);
        diag.describeIfSet(xmi ? "globalTransaction" : "global-transaction", global_transaction);
        diag.describeIfSet(xmi ? "webGlobalTransaction" : "web-global-transaction", web_global_transaction);
        diag.describeIfSet(xmi ? "markupLanguages" : "markup-language", markup_language);
    }
}
