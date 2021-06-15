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
package io.openliberty.restfulWS.client.security;

import javax.servlet.http.Cookie;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.web.WebSecurityHelper;

public class LtpaHandler {
    private static final TraceComponent tc = Tr.register(LtpaHandler.class);

    public static void configClientLtpaHandler(ClientRequestContext crc) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "configClientLtpaHandler - About to get a LTPA authentication token");
        }

        // retrieve a ltpa cookie from the Subject in current thread 
        try {
            // this interceptor must depend on the appSecurity feature to use WebSecurityHelper.getSSOCookieFromSSOToken()
            Cookie ssoCookie = WebSecurityHelper.getSSOCookieFromSSOToken();
            if (ssoCookie == null) {
                return;
            }
            String cookieName = ssoCookie.getName();
            String cookieValue = ssoCookie.getValue();
            if (cookieValue != null && !cookieValue.isEmpty() && cookieName != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Retrieved a LTPA authentication token. About to set a request cookie: " + cookieName + "=" + cookieValue);
                }
                crc.getHeaders().putSingle("Cookie", ssoCookie.getName() + "=" + ssoCookie.getValue());
            } else { // no user credential available
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cannot find a ltpa authentication token off of the thread, you may need enable feature appSecurity-2.0 or ssl-1.0");
                }
                //Because this is a client configuration property, we won't throws exception if it doesn't work, please analyze trace for detail
                //throw new ProcessingException("Cannot find a ltpa authentication token off of the thread");
            }
        } catch (NoClassDefFoundError ncdfe) {
            // expected if app security is not enabled
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "configClientLtpaHandler - caught NCDFE - expected if app security feature not enabled", ncdfe);
            }
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }
}