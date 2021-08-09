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
 * <p>This interface represents an Accept request-header. It can be used in to specify
 * media-ranges which are acceptable for the response. AcceptHeaders can be
 * used to indicate that the request is specifically limited to a small
 * set of desired types. The specification of the acceptable media
 * is split into type and subtype.
 * </p><p>
 * An AcceptHeader may be followed by one or more parameters applicable to the
 * media-range. Q-values allow the user to indicate the relative degree of
 * preference for that media-range, using the qvalue scale from 0 to 1. (If no
 * q-value is present, the media-range should be treated as having a q-value of 1.)
 * </p><p>
 * If no AcceptHeader is present in a Request, then it is assumed that the
 * client accepts media of type "application" and subType "sdp".
 * If an AcceptHeader is present, and if the server cannot send a response
 * which is acceptable according to the combined Accept field value, then
 * the server should send a ResponseMessage with a NOT_ACCEPTABLE
 * status code.
 * </p>
 *
 * @see ContentTypeHeader
 *
 * @version 1.0
 *
 */
public interface AcceptHeader extends ContentTypeHeader
{
    
    /**
     * Gets boolean value to indicate if the AcceptHeader
     * allows all media sub-types (i.e. content sub-type is "*")
     * @return boolean value to indicate if the AcceptHeader
     * allows all media sub-types
     */
    public boolean allowsAllContentSubTypes();
    
    /**
     * Sets q-value for media-range in AcceptHeader
     * Q-values allow the user to indicate the relative degree of
     * preference for that media-range, using the qvalue scale from 0 to 1.
     * (If no q-value is present, the media-range should be treated as having a q-value of 1.)
     * @param <var>qValue</var> q-value
     * @throws SipParseException if qValue is not accepted by implementation
     */
    public void setQValue(float qValue)
                 throws SipParseException;
    
    /**
     * Gets q-value of media-range in AcceptHeader
     * (Returns negative float if no q-value exists)
     * @return q-value of media-range
     */
    public float getQValue();
    
    /**
     * Gets boolean value to indicate if AcceptHeader
     * has q-value
     * @return boolean value to indicate if AcceptHeader
     * has q-value
     */
    public boolean hasQValue();
    
    /**
     * Removes q-value of media-range in AcceptHeader (if it exists)
     */
    public void removeQValue();
    
    /**
     * Gets boolean value to indicate if the AcceptHeader
     * allows all media types (i.e. content type is "*")
     * @return boolean value to indicate if the AcceptHeader
     * allows all media types
     */
    public boolean allowsAllContentTypes();
    
    /**
     * Name of AcceptHeader
     */
    public final static String name = "Accept";
}
