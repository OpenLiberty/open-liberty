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
package com.ibm.ws.security.authentication.tai;

import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.tai.internal.InterceptorConfigImpl;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 * This class process the TAI's properties as a Liberty feature.
 * Support properties are invokeBeforeSSO, invokeAfterSSO and disableLtpaCookie
 */
public class TAIUtil {
    private static final TraceComponent tc = Tr.register(TAIUtil.class);
    public static final String KEY_DISABLE_LTPA_COOKIE = "disableLtpaCookie";
    private boolean invokeBeforeSSO = false;
    private boolean invokeAfterSSO = false;
    private Object disableLtpaCookie = null;

    public TAIUtil(ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                   String interceptorId) {
        processUserFeatureTaiProps(interceptorServiceRef, interceptorId);
    }

    /**
     * This method will process the TAI's properties as a Liberty feature.
     * We do not have a direct access to the TAI as a Liberty feature configuration.
     *
     * @param interceptorServiceRef
     * @param interceptorId
     */
    public void processUserFeatureTaiProps(ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                           String interceptorId) {
        invokeBeforeSSO = false;
        invokeAfterSSO = false;
        disableLtpaCookie = false;

        ServiceReference<TrustAssociationInterceptor> taiServiceRef = interceptorServiceRef.getReference(interceptorId);

        Object beforeSsoProp = taiServiceRef.getProperty(InterceptorConfigImpl.KEY_INVOKE_BEFORE_SSO);
        Object afterSsoProp = taiServiceRef.getProperty(InterceptorConfigImpl.KEY_INVOKE_AFTER_SSO);
        disableLtpaCookie = taiServiceRef.getProperty(KEY_DISABLE_LTPA_COOKIE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "User feature interceptor properties of " + interceptorId);
            Tr.debug(tc, "beforeSsoProp=" + beforeSsoProp + " afterSsoProp=" + afterSsoProp + " disableLtpaCookie=" + disableLtpaCookie);
        }

        /*
         * The user feature TAI may have one the following attributes:
         * 1) Do not specified invokeBeforeSSO and invokeAfterSSO attributes.
         * 2) Specified only invokeAfterSSO attribute.
         * 3) Specified only invokeBeforeSSO attribute.
         * 4) Specified both invokeBeforeSSO and invokeAfterSSO attributes.
         */
        if (beforeSsoProp == null && afterSsoProp == null) {
            invokeAfterSSO = true;
        } else if (beforeSsoProp == null && afterSsoProp != null) {
            resolveOnlyInvokeAfterSSOSpecified(afterSsoProp);
        } else if (beforeSsoProp != null && afterSsoProp == null) {
            resolveOnlyInvokeBeforeSSOSpecified(beforeSsoProp);
        } else if (beforeSsoProp != null && afterSsoProp != null) {
            invokeBeforeSSO = (Boolean) beforeSsoProp;
            invokeAfterSSO = (Boolean) afterSsoProp;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "User feature interceptor properties:  ");
            Tr.debug(tc, "  invokeBeforeSSO=" + invokeBeforeSSO + " invokeAfterSSO=" + invokeAfterSSO + " disableLtpaCookie=" + disableLtpaCookie);
        }
    }

    /**
     * If invokeBeforeSSO is false, then this TAI will be invoked after SSO authentication
     *
     * @param beforeSSO
     * @return
     */
    protected void resolveOnlyInvokeBeforeSSOSpecified(Object beforeSSO) {
        invokeBeforeSSO = (Boolean) beforeSSO;
        if (!invokeBeforeSSO) {
            invokeAfterSSO = true;
        }
    }

    /**
     * If invokeAfterSSO is false, then this TAI will be invoked before SSO authentication.
     *
     * @param afterSSO
     * @return
     */
    protected void resolveOnlyInvokeAfterSSOSpecified(Object afterSSO) {
        invokeAfterSSO = (Boolean) afterSSO;
        if (!invokeAfterSSO) {
            invokeBeforeSSO = true;
        }
    }

    public boolean isInvokeBeforeSSO() {
        return invokeBeforeSSO;
    }

    public boolean isInvokeAfterSSO() {
        return invokeAfterSSO;
    }

    public Object isDisableLtpaCookie() {
        return disableLtpaCookie;
    }
}
