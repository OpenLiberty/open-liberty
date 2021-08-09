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
 * This interface represents the Expires entity-header.
 * The ExpiresHeader gives the date and time after which
 * the message content expires.
 * </p><p>
 * The ExpiresHeader is currently defined only for REGISTER and
 * INVITE Requests and their corresponding Responses.
 * In a REGISTER Request, the client indicates how long it wishes
 * the registration to be valid. In the Response, the server
 * indicates the earliest expiration time of all registrations. The server may
 * choose a shorter time interval than that requested by the client, but
 * should not choose a longer one.
 * </p><p>
 * For INVITE Requests the caller can limit the validity of an invitation, for
 * example, if a client wants to limit the time duration of a search or
 * a conference invitation. A user interface may take this as a hint to
 * leave the invitation window on the screen even if the user is not
 * currently at the workstation. This also limits the duration of a
 * search. If the INVITE Request expires before the search completes, the proxy
 * returns a REQUEST_TIMEOUT Response. In a MOVED_TEMPORARILY
 * Response, a server can advise the client of the maximal duration of
 * the redirection.
 * </p><p>
 * The value of this field can be either a date or an integer number
 * of seconds (in decimal), measured from the receipt of the Request.
 * The latter approach is preferable for short durations, as it does not
 * depend on clients and servers sharing a synchronized clock.
 * Implementations may treat values larger than 2**32-1 (4294967295 or
 * 136 years) as equivalent to 2**32-1.
 * </p>
 *
 * @version 1.0
 *
 */
public interface ExpiresHeader extends DateHeader
{
    
    /**
     * Sets expires of ExpiresHeader as delta-seconds
     * @param <var>deltaSeconds</var> delta-seconds
     * @throws SipParseException if deltaSeconds is not accepted by implementation
     */
    public void setDeltaSeconds(long deltaSeconds)
                 throws SipParseException;
    
    /**
     * Gets boolean value to indicate if expiry value of ExpiresHeader
     * is in date format
     * @return boolean value to indicate if expiry value of ExpiresHeader
     * is in date format
     */
    public boolean isDate();
    
    /**
     * Gets value of ExpiresHeader as delta-seconds
     * (Returns -1 if expires value is not in delta-second format)
     * @return value of ExpiresHeader as delta-seconds
     */
    public long getDeltaSeconds();
    
    //////////////////////////////////////////////////////////
    
    /**
     * Name of ExpiresHeader
     */
    public final static String name = "Expires";
}
