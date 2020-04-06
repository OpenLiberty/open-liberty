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
 * This interface represents the Timestamp general-header.
 * The TimeStampHeader describes when the client sent the
 * Request to the server. The value of the timestamp
 * is of significance only to the client and it may use any
 * timescale. The server must echo the exact same value and
 * may, if it has accurate information about this, add a
 * floating point number indicating the number of seconds
 * that have elapsed since it has received the Request.
 * The TimestampHeader is used by the client to compute the
 * round-trip time to the server so that it can adjust the
 * timeout value for retransmissions.
 *
 * @version 1.0
 *
 */
public interface TimeStampHeader extends Header
{
    
    /**
     * Gets delay of TimeStampHeader
     * (Returns neagative float if delay does not exist)
     * @return delay of TimeStampHeader
     */
    public float getDelay();
    
    /**
     * Gets boolean value to indicate if TimeStampHeader
     * has delay
     * @return boolean value to indicate if TimeStampHeader
     * has delay
     */
    public boolean hasDelay();
    
    /**
     * Sets timestamp of TimeStampHeader
     * @param <var>timeStamp</var> timestamp
     * @throws SipParseException if timeStamp is not accepted by implementation
     */
    public void setTimeStamp(float timeStamp)
                 throws SipParseException;
    
    /**
     * Sets delay of TimeStampHeader
     * @param <var>delay</var> delay
     * @throws SipParseException if delay is not accepted by implementation
     */
    public void setDelay(float delay)
                 throws SipParseException;
    
    /**
     * Removes delay from TimeStampHeader (if it exists)
     */
    public void removeDelay();
    
    /**
     * Gets timestamp of TimeStampHeader
     * @return timestamp of TimeStampHeader
     */
    public float getTimeStamp();
    
    /////////////////////////////////////////////////////////////
    
    /**
     * Name of TimeStampHeader
     */
    public final static String name = "Timestamp";
}
