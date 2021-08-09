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
 * This interface representshe Require request-header.
 * The RequireHeader is used by clients to tell user
 * agent servers about options that the client expects the server to
 * support in order to properly process the Request. If a server does
 * not understand the option, it must respond by returning a BAD_EXTENSION
 * Response and list those options it does not understand in
 * the UnsupportedHeader.
 * </p><p>
 * This is to make sure that the client-server interaction
 * will proceed without delay when all options are understood
 * by both sides, and only slow down if options are not
 * understood. For a well-matched client-server pair, the interaction
 * proceeds quickly, saving a round-trip often required by negotiation
 * mechanisms. In addition, it also removes ambiguity when the
 * client requires features that the server does not
 * understand. Some features, such as call handling fields,
 * are only of interest to end systems.
 * </p><p>
 * Proxy and redirect servers must ignore features that are not
 * understood. If a particular extension requires that intermediate
 * devices support it, the extension must be tagged in the ProxyRequireHeader
 * as well.
 *
 * @see ProxyRequireHeader
 *
 * @version 1.0
 *
 */
public interface RequireHeader extends OptionTagHeader
{
    
    /**
     * Name of RequireHeader
     */
    public final static String name = "Require";
}
