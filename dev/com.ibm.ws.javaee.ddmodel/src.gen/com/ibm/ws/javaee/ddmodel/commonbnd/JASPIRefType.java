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
package com.ibm.ws.javaee.ddmodel.commonbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class JASPIRefType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonbnd.JASPIRef {
    public JASPIRefType() {
        this(false);
    }

    public JASPIRefType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType provider_name;
    com.ibm.ws.javaee.dd.commonbnd.JASPIRef.UseJASPIEnum use_jaspi;

    @Override
    public java.lang.String getProviderName() {
        return provider_name != null ? provider_name.getValue() : null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonbnd.JASPIRef.UseJASPIEnum getUseJASPI() {
        return use_jaspi != null ? use_jaspi : com.ibm.ws.javaee.dd.commonbnd.JASPIRef.UseJASPIEnum.inherit;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ((xmi ? "providerName" : "provider-name").equals(localName)) {
                this.provider_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if ((xmi ? "useJaspi" : "use-jaspi").equals(localName)) {
                this.use_jaspi = parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.commonbnd.JASPIRef.UseJASPIEnum.class);
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
        diag.describeIfSet(xmi ? "providerName" : "provider-name", provider_name);
        diag.describeEnumIfSet(xmi ? "useJaspi" : "use-jaspi", use_jaspi);
    }
}
