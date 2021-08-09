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
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;


public class UsernameTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {
    private static final long serialVersionUID = -1507727324874727254L;
    protected static final TraceComponent tc = Tr.register(UsernameTokenInterceptorProvider.class,
                                                           WSSecurityConstants.TR_GROUP,
                                                           WSSecurityConstants.TR_RESOURCE_BUNDLE);
    private static final Collection<QName> ASSERTION_TYPES;
    static {
        ASSERTION_TYPES = new ArrayList<QName>();

        ASSERTION_TYPES.add(SP12Constants.USERNAME_TOKEN);
        ASSERTION_TYPES.add(SP11Constants.USERNAME_TOKEN);
    }

    public UsernameTokenInterceptorProvider() {
        this(new UsernameTokenInterceptor());
    }

    public UsernameTokenInterceptorProvider(Bus bus) {
        this((UsernameTokenInterceptor)
             bus.getProperty("org.apache.cxf.ws.security.usernametoken.interceptor"));
    }

    public UsernameTokenInterceptorProvider(UsernameTokenInterceptor inInterceptor) {
        super(ASSERTION_TYPES);
        this.getOutInterceptors().add(new UsernameTokenInterceptor());
        this.getInInterceptors().add(inInterceptor == null ? new UsernameTokenInterceptor() : inInterceptor);
        //not needed on fault chains
    }

}
