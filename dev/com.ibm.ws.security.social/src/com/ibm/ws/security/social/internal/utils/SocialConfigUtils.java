/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.security.social.SocialLoginService;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

public class SocialConfigUtils {

    public static final TraceComponent tc = Tr.register(SocialConfigUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public SSLSocketFactory getSSLSocketFactory(String uniqueId, SSLContext classSslContext, AtomicServiceReference<SocialLoginService> socialLoginServiceRef, String sslRef) throws SocialLoginException {
        SSLSocketFactory sslSocketFactory = null;
        if (classSslContext == null) {
            SocialLoginService service = socialLoginServiceRef.getService();
            if (service == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Social login service is not available");
                }
                return null;
            }
            SSLSupport sslSupport = service.getSslSupport();
            if (sslSupport == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "SSL support could not be found for social login service");
                }
                return null;
            }
            try {
                sslSocketFactory = sslSupport.getSSLSocketFactory(sslRef);
                JSSEHelper jsseHelper = sslSupport.getJSSEHelper();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "sslSocketFactory (" + sslRef + ") get: " + sslSocketFactory);
                }
            } catch (Exception e) {
                throw new SocialLoginException("FAILED_TO_GET_SSL_CONTEXT", e, new Object[] { uniqueId, e.getLocalizedMessage() });
            }
        }
        return sslSocketFactory;
    }

}
