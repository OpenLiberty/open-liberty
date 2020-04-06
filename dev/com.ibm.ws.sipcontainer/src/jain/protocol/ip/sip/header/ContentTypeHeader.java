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
 * This interface represents the Content-Type entity-header.
 * A ContentTypeHeader indicates the media type of the
 * entity-body sent to the recipient. There must be a ContentTypeHeader
 * included in Messages which contain a body. The media-type is
 * represented in the same manner as AcceptHeader.
 *
 * @see AcceptHeader
 *
 * @version 1.0
 *
 */
public interface ContentTypeHeader extends ParametersHeader
{
    
    /**
     * Gets media sub-type of ContentTypeHeader
     * @return media sub-type of ContentTypeHeader
     */
    public String getContentSubType();
    
    /**
     * Sets value of media subtype in ContentTypeHeader
     * @param <var>subType</var> media sub-type
     * @throws IllegalArgumentException if sub-type is null
     * @throws SipParseException if contentSubType is not accepted by implementation
     */
    public void setContentSubType(String contentSubType)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets value of media type in ContentTypeHeader
     * @param <var>type</var> media type
     * @throws IllegalArgumentException if type is null
     * @throws SipParseException if contentType is not accepted by implementation
     */
    public void setContentType(String ContentType)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets media type of ContentTypeHeader
     * @return media type of ContentTypeHeader
     */
    public String getContentType();
    
    ///////////////////////////////////////////////////////////////////
    
    /**
     * Name of ContentTypeHeader
     */
    public final static String name = "Content-Type";
}
