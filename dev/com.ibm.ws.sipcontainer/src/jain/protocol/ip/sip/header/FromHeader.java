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
 * This interface represents the From general-header.
 * Requests and Responses must contain a FromHeader,
 * indicating the initiator of the Request in its NameAddress.
 * The FromHeader may contain a tag. The server copies the FromHeader
 * from the Request to the Response. The optional "display-name"
 * of the NameAddress is meant to be rendered by a human-user interface.
 * A system should use the display name "Anonymous" if the identity of the
 * client is to remain hidden.
 * </p><p>
 * The SipURL of the NameAddress must not contain the transport, maddr,
 * ttl, or headers elements. A server that receives a SipURL
 * with these elements removes them before further processing.
 * </p><p>
 * The tag may appear in the FromHeader of a Request. It must be
 * present when it is possible that two instances of a user sharing a
 * address can make call invitations with the same CallIdHeader.
 * The tag value must be globally unique and cryptographically random
 * with at least 32 bits of randomness. A single user maintains the same
 * tag throughout the call identified by the CallIdHeader.
 * </p><p>
 * The CallIdHeader, ToHeader and FromHeader are needed to identify a call leg.
 * The distinction between call and call leg matters in calls
 * with multiple Responses to a forked Request.
 * </p>
 *
 * @version 1.0
 *
 */
public interface FromHeader extends EndPointHeader
{
    
    /**
     * Name of FromHeader
     */
    public final static String name = "From";
}
