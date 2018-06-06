/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter;

import javax.servlet.http.HttpServletRequest;

/**
 * A filter service for determining when an HTTP request should be intercepted.
 * <P>
 * The interface is used by the authentication type SPNEGO, SAML, OPENID ..etc.
 * A default implementation of this interface is provided. It
 * supports filtering rules that allow specification of coarse or fine-grained
 * criteria for determining when to intercept a given HTTP request.
 * <P>
 * 
 * @version 1.0.0
 */
public interface AuthenticationFilter {

    /**
     * Initializes the request filtering mechanism. The operation is invoked
     * when the interceptor is initialized if a custom filter is specified.
     * 
     * @param filter
     *            describes arbitrary filtering rules used by the filter to
     *            determine when an HTTP request should be intercepted
     * 
     * @return true if no errors were encountered while parsing the filter
     *         argument, false otherwise.
     */
    public boolean init(String filter);

    /**
     * Indicates whether or not the service should intercept the given request,
     * according to the filtering rules defined in the <CODE>init()</CODE>
     * operation.
     * 
     * @param request
     *            the HTTP request currently being processed.
     * 
     * @return true if the request matches the filtering criteria, otherwise
     *         false
     */
    public boolean isAccepted(HttpServletRequest request);

    /**
     * Indicates that all HTTP requests processed by the filter will be
     * intercepted.
     * 
     * @param all
     *            true causes the operation <CODE>isAccepted()</CODE> to
     *            unconditionally return true, otherwise
     *            <CODE>isAccepted()</CODE> applies the filtering rules in the
     *            <CODE>init()</CODE> operation.
     */
    public void setProcessAll(boolean all);
}
