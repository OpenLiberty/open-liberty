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
package jain.protocol.ip.sip.message;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.ToHeader;

import java.util.List;

/**
 * This interface provides factory methods to allow an application create Messages
 * from a particular JAIN SIP implementation.
 *
 * @version 1.0
 */
public interface MessageFactory
{
    
    /**
     * Creates Request with body
     * @param <var>requestURI</var> Request URI
     * @param <var>method</var> Request method
     * @param <var>callIdHeader</var> CallIdHeader
     * @param <var>cSeqHeader</var> CSeqHeader
     * @param <var>fromHeader</var> FromHeader
     * @param <var>toHeader</var> ToHeader
     * @param <var>viaHeaders</var> ViaHeaders
     * @param <var>body</var> body of Request
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if method or body are null, if requestURI,
     * callIdHeader, cSeqHeader, fromHeader, toHeader or contentTypeHeader are null or
     * not from same JAIN SIP implementation, or if viaHeaders is null, empty,
     * contains any null elements, or contains any objects that are not ViaHeaders
     * from the same JAIN SIP implementation
     * @throws SipParseException if method or body are not accepted by implementation
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List viaHeaders, String body, ContentTypeHeader contentTypeHeader)
                    throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates Request with body
     * @param <var>requestURI</var> Request URI
     * @param <var>method</var> Request method
     * @param <var>callIdHeader</var> CallIdHeader
     * @param <var>cSeqHeader</var> CSeqHeader
     * @param <var>fromHeader</var> FromHeader
     * @param <var>toHeader</var> ToHeader
     * @param <var>viaHeaders</var> ViaHeaders
     * @param <var>body</var> body of Request
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if method or body are null, if requestURI,
     * callIdHeader, cSeqHeader, fromHeader, toHeader or contentTypeHeader are null or
     * not from same JAIN SIP implementation, or if viaHeaders is null, empty,
     * contains any null elements, or contains any objects that are not ViaHeaders
     * from the same JAIN SIP implementation
     * @throws SipParseException if method or body are not accepted by implementation
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List viaHeaders, byte[] body, ContentTypeHeader contentTypeHeader)
                    throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates Response without body
     * @param <var>statusCode</var> status code
     * @param <var>callIdHeader</var> CallIdHeader
     * @param <var>cSeqHeader</var> CSeqHeader
     * @param <var>fromHeader</var> FromHeader
     * @param <var>toHeader</var> ToHeader
     * @param <var>viaHeaders</var> ViaHeaders
     * @throws IllegalArgumentException if callIdHeader,
     * cSeqHeader, fromHeader or toHeader are null or
     * not from same JAIN SIP implementation, or if viaHeaders is null, empty,
     * contains any null elements, or contains any objects that are not ViaHeaders
     * from the same JAIN SIP implementation
     * @throws SipParseException if statusCode is not accepted by implementation
     */
    public Response createResponse(int statusCode, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List viaHeaders)
                     throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates Request without body
     * @param <var>requestURI</var> Request URI
     * @param <var>method</var> Request method
     * @param <var>callIdHeader</var> CallIdHeader
     * @param <var>cSeqHeader</var> CSeqHeader
     * @param <var>fromHeader</var> FromHeader
     * @param <var>toHeader</var> ToHeader
     * @param <var>viaHeaders</var> ViaHeaders
     * @throws IllegalArgumentException if method is null, or if requestURI,
     * callIdHeader, cSeqHeader, fromHeader or toHeader are null or
     * not from same JAIN SIP implementation, or if viaHeaders is null, empty,
     * contains any null elements, or contains any objects that are not ViaHeaders
     * from the same JAIN SIP implementation
     * @throws SipParseException if method is not accepted by implementation
     */
    public Request createRequest(URI requestURI, String method, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List viaHeaders)
                    throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates Response with body
     * @param <var>statusCode</var> status code
     * @param <var>callIdHeader</var> CallIdHeader
     * @param <var>cSeqHeader</var> CSeqHeader
     * @param <var>fromHeader</var> FromHeader
     * @param <var>toHeader</var> ToHeader
     * @param <var>viaHeaders</var> ViaHeaders
     * @param <var>body</var> body of Request
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if body is null, or if callIdHeader,
     * cSeqHeader, fromHeader, toHeader or contentTypeHeader are null or
     * not from same JAIN SIP implementation, or if viaHeaders is null, empty,
     * contains any null elements, or contains any objects that are not ViaHeaders
     * from the same JAIN SIP implementation
     * @throws SipParseException if statusCode or body are not accepted by implementation
     */
    public Response createResponse(int statusCode, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List viaHeaders, String body, ContentTypeHeader contentTypeHeader)
                     throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates Response without body based on specified Request
     * @param <var>statusCode</var> status code
     * @param <var>request</var> Request to base Response on
     * @throws IllegalArgumentException if request is null or
     * not from same JAIN SIP implementation
     * @throws SipParseException if statusCode is not accepted by implementation
     */
    public Response createResponse(int statusCode, Request request)
                     throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates Response with body based on specified Request
     * @param <var>statusCode</var> status code
     * @param <var>request</var> Request to base Response on
     * @param <var>body</var> body of Request
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if body is null, or if request or
     * contentTypeHeader are null or not from same JAIN SIP implementation
     * @throws SipParseException if statusCode or body are not accepted by implementation
     */
    public Response createResponse(int statusCode, Request request, byte[] body, ContentTypeHeader contentTypeHeader)
                     throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates Response with body based on specified Request
     * @param <var>statusCode</var> status code
     * @param <var>request</var> Request to base Response on
     * @param <var>body</var> body of Request
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if body is null, or if request or
     * contentTypeHeader are null or not from same JAIN SIP implementation
     * @throws SipParseException if statusCode or body are not accepted by implementation
     */
    public Response createResponse(int statusCode, Request request, String body, ContentTypeHeader contentTypeHeader)
                     throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates Response with body
     * @param <var>statusCode</var> status code
     * @param <var>callIdHeader</var> CallIdHeader
     * @param <var>cSeqHeader</var> CSeqHeader
     * @param <var>fromHeader</var> FromHeader
     * @param <var>toHeader</var> ToHeader
     * @param <var>viaHeaders</var> ViaHeaders
     * @param <var>body</var> body of Request
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if body is null, or if callIdHeader,
     * cSeqHeader, fromHeader, toHeader or contentTypeHeader are null or
     * not from same JAIN SIP implementation, or if viaHeaders is null, empty,
     * contains any null elements, or contains any objects that are not ViaHeaders
     * from the same JAIN SIP implementation
     * @throws SipParseException if statusCode or body are not accepted by implementation
     */
    public Response createResponse(int statusCode, CallIdHeader callIdHeader, CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, List viaHeaders, byte[] body, ContentTypeHeader contentTypeHeader)
                     throws IllegalArgumentException,SipParseException;
}
