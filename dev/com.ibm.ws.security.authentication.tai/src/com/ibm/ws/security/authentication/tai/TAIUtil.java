/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;
import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

/**
 * This class process the attribute invokeBeforeSSO and invokeAfterSSO and other attributes
 * from the inerceptor user features
 */
public class TAIUtil {
    private static final TraceComponent tc = Tr.register(TAIUtil.class);

    private boolean invokeBeforeSSO = false;
    private boolean invokeAfterSSO = false;
    private boolean addLtpaCookieToResponse = true;

    /**
     * This method will process the TAI user feature properties and determine
     * whether this TAI will be invoked before or after the SSO authentication. Because
     * we do not have a direct access to the TAI user feature configuration if any.
     *
     * @param interceptorServiceRef
     * @param interceptorId
     */
    public void processTAIUserFeatureProps(ConcurrentServiceReferenceMap<String, TrustAssociationInterceptor> interceptorServiceRef,
                                           String interceptorId) {
        invokeBeforeSSO = false;
        invokeAfterSSO = false;
        addLtpaCookieToResponse = true;

        ServiceReference<TrustAssociationInterceptor> taiServiceRef = interceptorServiceRef.getReference(interceptorId);
        Object beforeSsoProp = taiServiceRef.getProperty(TAIConfig.KEY_INVOKE_BEFORE_SSO);
        Object afterSsoProp = taiServiceRef.getProperty(TAIConfig.KEY_INVOKE_AFTER_SSO);
        if (taiServiceRef.getProperty(TAIConfig.KEY_ADD_LTPA_TO_RESPONSE) != null) {
            addLtpaCookieToResponse = (Boolean) taiServiceRef.getProperty(TAIConfig.KEY_ADD_LTPA_TO_RESPONSE);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "user feature have properties,  beforeSsoProp=" + beforeSsoProp + " invokeAfterSSO=" + afterSsoProp +
                         " addLtpaCookieToResponse=" + addLtpaCookieToResponse);
        }

        /*
         * The interceptor user defined feature may have the following:
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
            Tr.debug(tc, "resolve user feature have,  invokeBeforeSSO=" + invokeBeforeSSO + " invokeAfterSSO=" + invokeAfterSSO +
                         " addLtpaCookieToResponse=" + addLtpaCookieToResponse);
        }
    }

    /**
     * If inbokeBeforeSSO is false, then this TAI will be invoked after SSO authentication
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

    public boolean addLtpaCookieToResponse() {
        return addLtpaCookieToResponse;
    }
}
