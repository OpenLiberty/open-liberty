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
 * This interface represents the Retry-After general-header.
 * The RetryAfterHeader can be used with a SERVICE_UNAVAILABLE
 * Response to indicate how long the service is expected to
 * be unavailable to the requesting client and with a NOT_FOUND,
 * BUSY, or DECLINE Response to indicate when the called
 * party anticipates being available again. The value of this field can
 * be either a date or an integer number of seconds (in decimal)
 * after the time of the Response.
 * </p><p>
 * A REGISTER Request may include a RetryAfterHeader when deleting
 * registrations with a wildcard ContactHeader. The value
 * then indicates when the user might again be reachable. The registrar
 * may then include this information in Responses to future calls.
 * </p><p>
 * An optional comment can be used to indicate additional information
 * about the time of callback. An optional duration indicates how long
 * the called party will be reachable starting at the initial time of
 * availability. If no duration is given, the service is assumed to
 * be available indefinitely.
 * </p>
 *
 * @version 1.0
 *
 */
public interface RetryAfterHeader extends ExpiresHeader
{
    
    /**
     * Gets boolean value to indicate if RetryAfterHeader
     * has comment
     * @return boolean value to indicate if RetryAfterHeader
     * has comment
     */
    public boolean hasComment();
    
    /**
     * Removes comment from RetryAfterHeader (if it exists)
     */
    public void removeComment();
    
    /**
     * Gets duration of RetryAfterHeader
     * (Returns negative long if duration does not exist)
     * @return duration of RetryAfterHeader
     */
    public long getDuration();
    
    /**
     * Gets comment of RetryAfterHeader
     * (Returns null if comment does not exist)
     * @return comment of RetryAfterHeader
     */
    public String getComment();
    
    /**
     * Removes duration from RetryAfterHeader (if it exists)
     */
    public void removeDuration();
    
    /**
     * Sets comment of RetryAfterHeader
     * @param <var>comment</var> comment
     * @throws IllegalArgumentException if comment is null
     * @throws SipParseException if comment is not accepted by implementation
     */
    public void setComment(String comment)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets duration of RetryAfterHeader
     * @param <var>duration</var> duration
     * @throws SipParseException if duration is not accepted by implementation
     */
    public void setDuration(long duration)
                 throws SipParseException;
    
    /**
     * Gets boolean value to indicate if RetryAfterHeader
     * has duration
     * @return boolean value to indicate if RetryAfterHeader
     * has duration
     */
    public boolean hasDuration();
    
    /////////////////////////////////////////////////////////
    
    /**
     * Name of RetryAfterHeader
     */
    public final static String name = "Retry-After";
}
