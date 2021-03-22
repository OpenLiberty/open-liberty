/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.io.Serializable;

import org.opensaml.saml.saml2.core.Assertion;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.sp.TraceConstants;
import com.ibm.ws.security.saml.sso20.token.Saml20TokenImpl;

/**
 *
 */
public class UserData implements Serializable {
    /**  */
    private static final long serialVersionUID = 1393140540843548788L;

    @SuppressWarnings("unused")
    private transient static final TraceComponent tc = Tr.register(UserData.class,
                                                                   TraceConstants.TRACE_GROUP,
                                                                   TraceConstants.MESSAGE_BUNDLE);
    Saml20Token samlToken;
    transient Assertion assertion;

    public UserData(Assertion assertion, String providerId) throws SamlException {
        this.samlToken = new Saml20TokenImpl(assertion, providerId);
        this.assertion = assertion;
    }

    public UserData(Assertion assertion, Saml20Token token) throws SamlException {
        this.samlToken = token;
        this.assertion = assertion;
    }

    public Saml20Token getSamlToken() {
        return samlToken;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SamlRequest [")
                        .append(" Saml20Token:").append(this.samlToken)
                        //.append(" xmlString:").append(this.xmlString)
                        .append("]");
        return sb.toString();
    }
}
