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

public class ResourceEnvRefType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef {
    public ResourceEnvRefType() {
        this(false);
    }

    public ResourceEnvRefType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType bindingResourceEnvRef;
    com.ibm.ws.javaee.ddmodel.StringType binding_name;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public java.lang.String getBindingName() {
        return binding_name != null ? binding_name.getValue() : null;
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
            if ((xmi ? "jndiName" : "binding-name").equals(localName)) {
                this.binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "bindingResourceEnvRef".equals(localName)) {
            this.bindingResourceEnvRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("bindingResourceEnvRef", parser.crossComponentDocumentType);
            parser.parse(bindingResourceEnvRef);
            com.ibm.ws.javaee.dd.common.ResourceEnvRef referent = this.bindingResourceEnvRef.resolveReferent(parser, com.ibm.ws.javaee.dd.common.ResourceEnvRef.class);
            if (referent == null) {
                DDParser.unresolvedReference("bindingResourceEnvRef", this.bindingResourceEnvRef.getReferenceString());
            } else {
                this.name = parser.parseString(referent.getName());
            }
            return true;
        }
        return false;
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            diag.describeIfSet("bindingResourceEnvRef", bindingResourceEnvRef);
        } else {
            diag.describeIfSet("name", name);
        }
        diag.describeIfSet(xmi ? "jndiName" : "binding-name", binding_name);
    }
}
