/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wssecurity.cxf.interceptor;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class WSSecurityLibertyPluginProvider extends AbstractPolicyInterceptorProvider {
    private static final TraceComponent tc = Tr.register(WSSecurityLibertyPluginProvider.class);

    private static final long serialVersionUID = -1507727324874727254L;
    private static final Collection<QName> ASSERTION_TYPES;
    static {
        ASSERTION_TYPES = new ArrayList<QName>();

        ASSERTION_TYPES.add(SP12Constants.USERNAME_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.SAML_TOKEN);
        ASSERTION_TYPES.add(SP12Constants.X509_TOKEN); // ??? Do we need to add some more specific X509 QName?
        ASSERTION_TYPES.add(SP12Constants.TRANSPORT_BINDING);
        ASSERTION_TYPES.add(SP12Constants.ASYMMETRIC_BINDING);
        ASSERTION_TYPES.add(SP12Constants.SYMMETRIC_BINDING);
        //v3
        ASSERTION_TYPES.add(SP12Constants.SIGNED_PARTS);

        ASSERTION_TYPES.add(SP11Constants.TRANSPORT_BINDING);
        ASSERTION_TYPES.add(SP11Constants.ASYMMETRIC_BINDING);
        ASSERTION_TYPES.add(SP11Constants.SYMMETRIC_BINDING);
        ASSERTION_TYPES.add(SP11Constants.SIGNED_PARTS);
    }

    public WSSecurityLibertyPluginProvider() {

        this(new WSSecurityLibertyPluginInterceptor());
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.getClass().getName() + " default constructor() exit");
        };
    }

    public WSSecurityLibertyPluginProvider(WSSecurityLibertyPluginInterceptor inInterceptor) {

        super(ASSERTION_TYPES);
        this.getOutInterceptors().add(new WSSecurityLibertyPluginInterceptor());
        this.getInInterceptors().add(inInterceptor == null ? new WSSecurityLibertyPluginInterceptor() : inInterceptor);
        //not needed on fault chains
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.getClass().getName() + " constructor (WSSecurityLibertyPluginInterceptor) exit");
        };
    }
}
