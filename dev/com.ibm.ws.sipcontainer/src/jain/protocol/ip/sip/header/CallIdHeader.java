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
 * This interface represents the Call-ID general-header.
 * CallIdHeader uniquely identifies a particular
 * invitation or all registrations of a particular client. Note that a
 * single multimedia conference can give rise to several calls with
 * different Call-IDs, e.g., if a user invites a single individual
 * several times to the same (long-running) conference.
 * </p><p>
 * For an INVITE Request, a callee user agent server should not alert
 * the user if the user has responded previously to the Call-ID in the
 * INVITE Request. If the user is already a member of the conference and
 * the conference parameters contained in the session description have
 * not changed, a callee user agent server may  silently accept the call,
 * regardless of the Call-ID. An invitation for an existing Call-ID or
 * session can change the parameters of the conference. A client
 * application MAY decide to simply indicate to the user that the
 * conference parameters have been changed and accept the invitation
 * automatically or it may require user confirmation.
 * </p><p>
 * A user may be invited to the same conference or call using several
 * different CallIdHeaders. If desired, the client may use identifiers within
 * the session description to detect this duplication. For example, SDP
 * contains a session id and version number in the origin (o) field.
 * </p><p>
 * The REGISTER and OPTIONS Requests use the CallIdHeader to
 * unambiguously match Requests and Responses. All REGISTER Requests
 * issued by a single client should use the same CallIdHeader, at least
 * within the same boot cycle.
 *
 * @version 1.0
 *
 */
public interface CallIdHeader extends Header
{
    
    /**
     * Sets Call-Id of CallIdHeader
     * @param <var>callId</var> Call-Id
     * @throws IllegalArgumentException if callId is null
     * @throws SipParseException if callId is not accepted by implementation
     */
    public void setCallId(String callId)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets Call-Id of CallIdHeader
     * @return Call-Id of CallIdHeader
     */
    public String getCallId();
    
    ////////////////////////////////////////////////////
    
    /**
     * Name of CallIdHeader
     */
    public final static String name = "Call-ID";
}
