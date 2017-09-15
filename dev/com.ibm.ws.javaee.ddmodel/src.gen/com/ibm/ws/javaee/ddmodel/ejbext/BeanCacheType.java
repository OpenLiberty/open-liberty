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

public class BeanCacheType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.ejbext.BeanCache {
    public BeanCacheType() {
        this(false);
    }

    public BeanCacheType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.dd.ejbext.BeanCache.ActivationPolicyTypeEnum activation_policy;

    @Override
    public com.ibm.ws.javaee.dd.ejbext.BeanCache.ActivationPolicyTypeEnum getActivationPolicy() {
        return activation_policy;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ((xmi ? "activateAt" : "activation-policy").equals(localName)) {
                this.activation_policy = parser.parseEnumAttributeValue(index, com.ibm.ws.javaee.dd.ejbext.BeanCache.ActivationPolicyTypeEnum.class);
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
        diag.describeEnumIfSet(xmi ? "activateAt" : "activation-policy", activation_policy);
    }
}
