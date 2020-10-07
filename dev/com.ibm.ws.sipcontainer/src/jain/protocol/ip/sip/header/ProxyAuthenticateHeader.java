/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip.header;

/**
 * <p>
 * This interface represents the Proxy-Authenticate response-header.
 * A ProxyAuthenticateHeader must be included as part
 * of a PROXY_AUTHENTICATION_REQUIRED Response. The field value
 * consists of a challenge that indicates the authentication scheme and
 * parameters applicable to the proxy for this RequestURI.
 * </p><p>
 * Note - Unlike its usage within HTTP, the ProxyAuthenticateHeader must be
 * passed upstream in the Response to the UAC. In SIP, only UAC's can
 * authenticate themselves to proxies.
 * </p><p>
 * A client should cache the credentials used for a particular proxy
 * server and realm for the next Request to that server. Credentials
 * are, in general, valid for a specific value of the RequestURI at a
 * particular proxy server. If a client contacts a proxy server that has
 * required authentication in the past, but the client does not have
 * credentials for the particular RequestURI, it may attempt to use the
 * most-recently used credential. The server responds with an UNAUTHORIZED
 * Response if the client guessed incorrectly.
 * </p><p>
 * This suggested caching behavior is motivated by proxies
 * restricting phone calls to authenticated users. It seems
 * likely that in most cases, all destinations require the
 * same password. Note that end-to-end authentication is
 * likely to be destination-specific.
 * </p>
 *
 * @version 1.0
 *
 */
public interface ProxyAuthenticateHeader extends SecurityHeader
{
    
    /**
     * Name of ProxyAuthenticateHeader
     */
    public final static String name = "Proxy-Authenticate";
}
