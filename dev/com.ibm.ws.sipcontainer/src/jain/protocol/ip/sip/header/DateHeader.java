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

import java.util.Date;

/**
 * <p>
 * This interface represents the Date general header.
 * DateHeader reflects the time when a Request or Response
 * is first sent. Thus, retransmissions have the same Date header field
 * value as the original.
 * </p><p>
 * The DateHeader can be used by simple end systems
 * without a battery-backed clock to acquire a notion of
 * current time.
 * </p>
 *
 * @version 1.0
 *
 */
public interface DateHeader extends Header
{
    
    /**
     * Sets date of DateHeader
     * @param <var>date</var> date
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public void setDate(Date date)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets date of DateHeader
     * @param <var>date</var> date String
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public void setDate(String date)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets date of DateHeader
     * (Returns null if date does not exist (this can only apply
     * for ExpiresHeader subinterface - when the expires value is in delta
     * seconds format)
     * @return date of DateHeader
     */
    public Date getDate();
    
    ////////////////////////////////////////////////////////////////
    
    /**
     * Name of DateHeader
     */
    public final static String name = "Date";
}
