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

public class MarkupLanguageType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.webext.MarkupLanguage {
    public MarkupLanguageType() {
        this(false);
    }

    public MarkupLanguageType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    com.ibm.ws.javaee.ddmodel.StringType mime_type;
    com.ibm.ws.javaee.ddmodel.StringType error_page;
    com.ibm.ws.javaee.ddmodel.StringType default_page;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.PageType, com.ibm.ws.javaee.dd.webext.Page> page;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public java.lang.String getMimeType() {
        return mime_type != null ? mime_type.getValue() : null;
    }

    @Override
    public java.lang.String getErrorPage() {
        return error_page != null ? error_page.getValue() : null;
    }

    @Override
    public java.lang.String getDefaultPage() {
        return default_page != null ? default_page.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.webext.Page> getPages() {
        if (page != null) {
            return page.getList();
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
            // "name" is the same for XML and XMI.
            if ("name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
            if ((xmi ? "mimeType" : "mime-type").equals(localName)) {
                this.mime_type = parser.parseStringAttributeValue(index);
                return true;
            }
            if ((xmi ? "errorPage" : "error-page").equals(localName)) {
                this.error_page = parser.parseStringAttributeValue(index);
                return true;
            }
            if ((xmi ? "defaultPage" : "default-page").equals(localName)) {
                this.default_page = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if ((xmi ? "pages" : "page").equals(localName)) {
            com.ibm.ws.javaee.ddmodel.webext.PageType page = new com.ibm.ws.javaee.ddmodel.webext.PageType(xmi);
            parser.parse(page);
            this.addPage(page);
            return true;
        }
        return false;
    }

    void addPage(com.ibm.ws.javaee.ddmodel.webext.PageType page) {
        if (this.page == null) {
            this.page = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.webext.PageType, com.ibm.ws.javaee.dd.webext.Page>();
        }
        this.page.add(page);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("name", name);
        diag.describeIfSet(xmi ? "mimeType" : "mime-type", mime_type);
        diag.describeIfSet(xmi ? "errorPage" : "error-page", error_page);
        diag.describeIfSet(xmi ? "defaultPage" : "default-page", default_page);
        diag.describeIfSet(xmi ? "pages" : "page", page);
    }
}
