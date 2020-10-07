/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
import org.apache.cxf.ws.security.policy.SP12Constants;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;

@SuppressWarnings("serial")
public class WSSecuritySamlTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(WSSecuritySamlTokenInterceptorProvider.class,
                                                         WSSecurityConstants.TR_GROUP,
                                                         WSSecurityConstants.TR_RESOURCE_BUNDLE);
    private static final Collection<QName> ASSERTION_TYPES;
    static {
        ASSERTION_TYPES = new ArrayList<QName>();
        ASSERTION_TYPES.add(SP12Constants.SAML_TOKEN);
    }

    public WSSecuritySamlTokenInterceptorProvider() {
        super(ASSERTION_TYPES);

        this.getOutInterceptors().add(new WSSecuritySamlTokenInterceptor());
        this.getInInterceptors().add(new WSSecuritySamlTokenInterceptor());
    }

}
