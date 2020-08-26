/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.appsecurity.component;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * DON"T EVER USE A CLASS LIKE THIS UNLESS YOU NEED TO ACCESS A SERVICE FROM A NON-OSGI CONTEXT
 * and you understand the lifecycle issues involved and have dealt with them.
 */

@Component(name = "com.ibm.ws.jaxrs20.appsecurity.component.SSLSupportService", property = { "service.vendor=IBM" })
public class SSLSupportService {

    private static final TraceComponent tc = Tr.register(SSLSupportService.class);
    private static volatile SSLSupport sslSupport;

    @Reference(name = "SSLSupportService",
               service = SSLSupport.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setSSLSupportService(SSLSupport service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "registerSSLSupportService");
        }
        sslSupport = service;
    }

    protected void unsetSSLSupportService(SSLSupport service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "unregisterSSLSupportService");
        }
        if (sslSupport == service)
            sslSupport = null;
    }

    public static boolean isSSLSupportServiceReady() {

        return (sslSupport != null) ? true : false;
    }

    public static SSLSupport getSSLSupport() {

        return sslSupport;
    }

    public static SSLSocketFactory getSSLSocketFactory(String sslRef) throws SSLException {
        return sslSupport.getSSLSocketFactory(sslRef);
    }
}
