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
 * This interface represents the Proxy-Authorization request-header.
 * The ProxyAuthorizationHeader allows the client to
 * identify itself (or its user) to a proxy which requires
 * authentication. The ProxyAuthorizationHeader value consists of
 * credentials containing the authentication information of the user
 * agent for the proxy and/or realm of the resource being requested.
 * </p><p>
 * Unlike the AuthorizationHeader, the ProxyAuthorizationHeader applies
 * only to the next outbound proxy that demanded authentication using
 * the ProxyAuthenticateHeader. When multiple proxies are used in a
 * chain, the ProxyAuthorizationHeader is consumed by the first
 * outbound proxy that was expecting to receive credentials. A proxy may
 * relay the credentials from the client Request to the next proxy if
 * that is the mechanism by which the proxies cooperatively authenticate
 * a given Request.
 *
 * @version 1.0
 *
 */
public interface ProxyAuthorizationHeader extends SecurityHeader
{
    
    /**
     * Name of ProxyAuthorizationHeader
     */
    public final static String name = "Proxy-Authorization";
}
