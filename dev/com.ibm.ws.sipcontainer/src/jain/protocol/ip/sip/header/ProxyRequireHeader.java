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
 * This interface represents the Proxy-Require request-header.
 * The ProxyRequireHeader is used to indicate proxy-sensitive
 * features that must be supported by the proxy. Any ProxyRequireHeader
 * features that are not supported by the proxy must be
 * negatively acknowledged by the proxy to the client if not supported.
 * Proxy servers treat this field identically to the RequireHeader.
 *
 * @see RequireHeader
 *
 * @version 1.0
 *
 */
public interface ProxyRequireHeader extends OptionTagHeader
{
    
    /**
     * Name of ProxyRequireHeader
     */
    public final static String name = "Proxy-Require";
}
