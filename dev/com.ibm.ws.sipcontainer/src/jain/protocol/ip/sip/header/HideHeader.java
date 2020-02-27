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

import jain.protocol.ip.sip.SipParseException;

/**
 * <p>
 * This interface represents the Hide request-header.
 * A client uses the HideHeader to indicate that it wants
 * the path comprised of the ViaHeaders to be hidden from subsequent
 * proxies and user agents. It can take two forms: route HideHeader and hop HideHeader.
 * HideHeaders are typically added by the client user agent, but may be
 * added by any proxy along the path.
 * </p><p>
 * If a Request contains the route HideHeader, all following
 * proxies should hide their previous hop. If a Request contains the
 * hop HideHeader, only the next proxy should hide the
 * previous hop and then remove the HideHeader unless it also wants to
 * remain anonymous.
 * </p><p>
 * A server hides the previous hop by encrypting the host and port
 * of the top-most ViaHeader with an algorithm of its
 * choice. Servers should add additional "salt" to the host and port
 * information prior to encryption to prevent malicious downstream
 * proxies from guessing earlier parts of the path based on seeing
 * identical encrypted ViaHeaders. Hidden ViaHeaders are marked with
 * the hidden ViaHeader option.
 * </p><p>
 * A server that is capable of hiding ViaHeaders must attempt to
 * decrypt all ViaHeaders marked as hidden to perform loop detection.
 * Servers that are not capable of hiding can ignore hidden ViaHeaders
 * in their loop detection algorithm.
 * </p><p>
 * If hidden ViaHeaders were not marked, a proxy would have to
 * decrypt all ViaHeaders to detect loops, just in case one was
 * encrypted, as the hop HideHeader may have been removed
 * along the way.
 * </p><p>
 * A host must not add such a hop HideHeader unless it can
 * guarantee it will only send a Request for this destination to the
 * same next hop. The reason for this is that it is possible that the
 * Request will loop back through this same hop from a downstream proxy.
 * he loop will be detected by the next hop if the choice of next hop
 * is fixed, but could loop an arbitrary number of times otherwise.
 * </p><p>
 * A client requesting with a route HideHeader can only rely on keeping the
 * Request path private if it sends the Request to a trusted proxy.
 * Hiding the route of a SIP Request is of limited value if the Request
 * results in data packets being exchanged directly between the calling
 * and called user agent.
 * </p><p>
 * The use of HideHeaders is discouraged unless path privacy is
 * truly needed; HideHeaders impose extra processing costs and
 * restrictions for proxies and can cause Requests to generate LOOP_DETECTED
 * Responses that could otherwise be avoided.
 * </p>
 *
 * @version 1.0
 *
 */
public interface HideHeader extends Header
{
    
    /**
     * Sets hide value of HideHeader
     * @param <var>hide</var> hide value of HideHeader
     * @throws IllegalArgumentException if hide is null
     * @throws SipParseException if hide is not accepted by implementation
     */
    public void setHide(String hide)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Returns hide value of HideHeader
     * @return hide value of HideHeader
     */
    public String getHide();
    
    /**
     * Hop hide constant
     */
    public final static String HIDE_HOP = "hop";
    
    /**
     * Name of HideHeader
     */
    public final static String name = "Hide";
    
    ////////////////////////////////////////////////////////////////
    
    /**
     * Route hide constant
     */
    public final static String HIDE_ROUTE = "route";
}
