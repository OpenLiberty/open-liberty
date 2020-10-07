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
 * This interface represents the Content-Length entity-header.
 * ContentLengthHeader indicates the size of the
 * message-body, in decimal number of octets, sent to the recipient.
 * </p><p>
 * Applications should use this field to indicate the size of the
 * message-body to be transferred, regardless of the media type of the
 * entity. Any Content-Length greater than or equal to zero is a valid
 * value. If no body is present in a Message, then the Content-Length
 * header field must be set to zero.
 * </p>
 *
 * @version 1.0
 *
 */
public interface ContentLengthHeader extends Header
{
    
    /**
     * Gets content-length of ContentLengthHeader
     * @return content-length of ContentLengthHeader
     */
    public int getContentLength();
    
    /**
     * Set content-length of ContentLengthHeader
     * @param <var>contentLength</var> content-length
     * @throws SipParseException if contentLength is not accepted by implementation
     */
    public void setContentLength(int contentLength)
                 throws SipParseException;
    
    //////////////////////////////////////////////////////////////
    
    /**
     * Name of ContentLengthHeader
     */
    public final static String name = "Content-Length";
}
