/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.webcontainer.internalRuntimeExport.srt.IPrivateRequestAttributes;

/**
 * Utility class for handling SRTServletRequest objects.
 */
public class SRTServletRequestUtils {
    private static final TraceComponent tc = Tr.register(SRTServletRequestUtils.class);

    /**
     * Get private attribute value from the request (if applicable).
     * If the request object is of an unexpected type, a {@code null} is returned.
     * 
     * @param req
     * @param key
     * @return The attribute value, or {@code null} if the object type is unexpected
     * @see IPrivateRequestAttributes#getPrivateAttribute(String)
     */
    public static Object getPrivateAttribute(HttpServletRequest req, String key) {
        HttpServletRequest sr = getWrappedServletRequestObject(req);
        if (sr instanceof IPrivateRequestAttributes) {
            return ((IPrivateRequestAttributes) sr).getPrivateAttribute(key);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getPrivateAttribute called for non-IPrivateRequestAttributes object", req);
            }
            return null;
        }
    }

    /**
     * Set a private attribute in the request (if applicable).
     * If the request object is of an unexpected type, no operation occurs.
     * 
     * @param req
     * @param key
     * @param object
     * @see IPrivateRequestAttributes#setPrivateAttribute(String, Object)
     */
    public static void setPrivateAttribute(HttpServletRequest req, String key, Object object) {
        HttpServletRequest sr = getWrappedServletRequestObject(req);
        if (sr instanceof IPrivateRequestAttributes) {
            ((IPrivateRequestAttributes) sr).setPrivateAttribute(key, object);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getPrivateAttribute called for non-IPrivateRequestAttributes object", req);
            }
        }
    }

    /**
     * Remove a private attribute from the request (if applicable).
     * If the request object is of an unexpected type, no operation occurs.
     * 
     * @param req
     * @param key
     * @see IPrivateRequestAttributes#removePrivateAttribute(String)
     */
    public static void removePrivateAttribute(HttpServletRequest req, String key) {
        HttpServletRequest sr = getWrappedServletRequestObject(req);
        if (sr instanceof IPrivateRequestAttributes) {
            ((IPrivateRequestAttributes) sr).removePrivateAttribute(key);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getPrivateAttribute called for non-IPrivateRequestAttributes object", req);
            }
        }
    }

    /**
     * Obtain the value of the specified header.
     * 
     * @param req
     * @param key
     * @return The value of the header
     * @see HttpServletRequest#getHeader(String)
     */
    @Sensitive
    public static String getHeader(HttpServletRequest req, String key) {
        HttpServletRequest sr = getWrappedServletRequestObject(req);
        return sr.getHeader(key);
    }

    /**
     * Drill down through any possible HttpServletRequestWrapper objects.
     * 
     * @param sr
     * @return
     */
    private static HttpServletRequest getWrappedServletRequestObject(HttpServletRequest sr) {
        if (sr instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper w = (HttpServletRequestWrapper) sr;
            // make sure we drill all the way down to an SRTServletRequest...there
            // may be multiple proxied objects
            sr = (HttpServletRequest) w.getRequest();
            while (sr instanceof HttpServletRequestWrapper)
                sr = (HttpServletRequest) ((HttpServletRequestWrapper) sr).getRequest();
        }
        return sr;
    }

}
