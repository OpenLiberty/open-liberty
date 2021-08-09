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
 * This interface represents the Record-Route general-header.
 * The RecordRouteHeader is added to a Request by any proxy
 * that insists on being in the path of subsequent Requests for
 * the same call leg. It contains a globally reachable RequestURI that
 * identifies the proxy server. Each proxy server adds its RequestURI
 * to the beginning of the list.
 * </p><p>
 * The server copies the RecordRouteHeader unchanged into the
 * Response. (RecordRouteHeader is only relevant for SUCCESS Responses.)
 * </p><p>
 * The calling user agent client copies the RecordRouteHeaders into
 * RouteHeaders of subsequent Requests within the same call leg,
 * reversing the order, so that the first entry is closest
 * to the user agent client. If the Response contained a ContactHeader
 * field, the calling user agent adds its content as the last RouteHeader.
 * Unless this would cause a loop, any client must send any
 * subsequent Requests for this call leg to the RequestURI in the first
 * RouteHeader and remove that entry.
 * </p><p>
 * The calling user agent must not use the RecordRouteHeaders in
 * Requests that contain RouteHeaders.
 * </p><p>
 * Some proxies, such as those controlling firewalls or in an
 * automatic call distribution (ACD) system, need to maintain
 * call state and thus need to receive any BYE and ACK Requests
 * for the call.
 * </p><p>
 * Proxy servers should use the SipURL maddr in the NameAddress containing their
 * address to ensure that subsequent Requests are guaranteed to reach
 * exactly the same server.
 * </p>
 *
 * @see RouteHeader
 *
 * @version 1.0
 *
 */
public interface RecordRouteHeader extends NameAddressHeader
{
    
    /**
     * Name of RecordRouteHeader
     */
    public final static String name = "Record-Route";
}
