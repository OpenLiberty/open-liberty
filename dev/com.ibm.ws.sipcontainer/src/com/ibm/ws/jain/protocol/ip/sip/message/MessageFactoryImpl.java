/*******************************************************************************
 * Copyright (c) 2003,2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jain.protocol.ip.sip.message;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.URI;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContentLengthHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.message.MessageFactory;
import jain.protocol.ip.sip.message.Request;
import jain.protocol.ip.sip.message.Response;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.jain.protocol.ip.sip.header.HeaderFactoryImpl;
import com.ibm.ws.jain.protocol.ip.sip.header.ViaHeaderImpl;

/**
 * Message factory implementation.
 * 
 * @author Assaf Azaria, April 2003.
 */
public class MessageFactoryImpl implements MessageFactory
{
	/** 
     */ 
    public MessageFactoryImpl()
    {}

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
     * @throws IllegalArgumentException if method or body are null,
     * if requestURI, callIdHeader, cSeqHeader, fromHeader, toHeader or
     * contentTypeHeader are null or
     * not from same JAIN SIP implementation, or if viaHeaders is null, empty,
     * contains any null elements, or contains any objects that are
     * not ViaHeaders from the same JAIN SIP implementation
     * @throws SipParseException if method or body are not accepted by
     * implementation
     */
    public Request createRequest(URI requestURI, String method,
        CallIdHeader callIdHeader,CSeqHeader cSeqHeader,
        FromHeader fromHeader,ToHeader toHeader,
        List viaHeaders, String body,
        ContentTypeHeader contentTypeHeader)
        throws IllegalArgumentException, SipParseException
    {
		Request req = createRequest(
			requestURI,
			method,
			callIdHeader,
			cSeqHeader,
			fromHeader,
			toHeader,
			viaHeaders);
		req.setBody(body, contentTypeHeader);
		return req;
    }

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
     * @throws IllegalArgumentException if method or body are null, 
     * if requestURI, callIdHeader, cSeqHeader, fromHeader, toHeader 
     * or contentTypeHeader are null or not from same JAIN SIP implementation, 
     * or if viaHeaders is null, empty, contains any null elements, 
     * or contains any objects that are not ViaHeaders
     * from the same JAIN SIP implementation
     * @throws SipParseException if method or body are 
     * not accepted by implementation.
     */
    public Request createRequest(
        URI requestURI,
        String method,
        CallIdHeader callIdHeader,
        CSeqHeader cSeqHeader,
        FromHeader fromHeader,
        ToHeader toHeader,
        List viaHeaders,
        byte body[],
        ContentTypeHeader contentTypeHeader)
        throws IllegalArgumentException, SipParseException
    {
		Request req = createRequest(
			requestURI,
			method,
			callIdHeader,
			cSeqHeader,
			fromHeader,
			toHeader,
			viaHeaders);
		req.setBody(body, contentTypeHeader);
		return req;
    }

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
    public Response createResponse(
        int statusCode,
        CallIdHeader callIdHeader,
        CSeqHeader cSeqHeader,
        FromHeader fromHeader,
        ToHeader toHeader,
        List viaHeaders)
        throws IllegalArgumentException, SipParseException
    {

        if (callIdHeader == null || cSeqHeader == null || 
        	fromHeader == null || toHeader == null || 
        	viaHeaders == null || viaHeaders.isEmpty())
        {
        	throw new IllegalArgumentException("MessageFactory: null argument"); 
        } 

		// Check for null in via headers.
		for (int i = 0; i < viaHeaders.size(); i++)
		{
			Object obj = viaHeaders.get(i);
			if (obj == null)
			{   
				throw new IllegalArgumentException("Message Factory: Null via headers"); 
			} 
			if (!(obj instanceof ViaHeaderImpl))
			{
				throw new IllegalArgumentException("MessageFactory: Via Headers in bad format"); 
			} 
		}        
		
		ResponseImpl resImpl = new ResponseImpl();
        
        resImpl.setStatusCode(statusCode);
        resImpl.setCallIdHeader(callIdHeader);
        resImpl.setCSeqHeader(cSeqHeader);
        resImpl.setFromHeader(fromHeader);
        resImpl.setToHeader(toHeader);
        resImpl.setViaHeaders(viaHeaders);

        return resImpl;
    }

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
    public Request createRequest(
        URI requestURI,
        String method,
        CallIdHeader callIdHeader,
        CSeqHeader cSeqHeader,
        FromHeader fromHeader,
        ToHeader toHeader,
        List viaHeaders)
        throws IllegalArgumentException, SipParseException
    {

        if (requestURI == null || method == null || callIdHeader == null || 
        	cSeqHeader == null || fromHeader == null || toHeader == null || 
        	viaHeaders == null || viaHeaders.isEmpty())
        {
			throw new IllegalArgumentException("Message Factory: null argument");
        }
        

		// Check for null in via headers.
		for (int i = 0; i < viaHeaders.size(); i++)
		{
			Object obj = viaHeaders.get(i);
			if (obj == null)
			{   
				throw new IllegalArgumentException("Message Factory: Null via headers"); 
			} 
			if (!(obj instanceof ViaHeaderImpl))
			{
				throw new IllegalArgumentException("MessageFactory: Via Headers in bad format"); 
			} 
		}        

        boolean isCancel = method != null && method.equalsIgnoreCase(Request.CANCEL);
		RequestImpl reqImpl = isCancel
			? new CancelRequest()
			: new RequestImpl();
        reqImpl.setRequestURI(requestURI);
        reqImpl.setMethod(method);
        reqImpl.setCSeqHeader(cSeqHeader);
        reqImpl.setCallIdHeader(callIdHeader);
        reqImpl.setFromHeader(fromHeader);
        reqImpl.setToHeader(toHeader);
        reqImpl.setViaHeaders(viaHeaders);

        return reqImpl;
    }

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
     * contains any null elements, or contains any objects that are not
     *  ViaHeaders from the same JAIN SIP implementation
     * @throws SipParseException if statusCode or body are not accepted by
     *  implementation
     */
    public Response createResponse(
        int statusCode,
        CallIdHeader callIdHeader,
        CSeqHeader cSeqHeader,
        FromHeader fromHeader,
        ToHeader toHeader,
        List viaHeaders,
        String body,
        ContentTypeHeader contentTypeHeader)
        throws IllegalArgumentException, SipParseException
    {

        if (callIdHeader == null || cSeqHeader == null || fromHeader == null ||
            toHeader == null || viaHeaders == null || viaHeaders.isEmpty() || 
            body == null || contentTypeHeader == null)
		{
			throw new IllegalArgumentException("Message Factory: null argument"); 
		} 

		// Check for null in via headers.
		for (int i = 0; i < viaHeaders.size(); i++)
		{
			Object obj = viaHeaders.get(i);
			if (obj == null)
			{   
				throw new IllegalArgumentException("Message Factory: Null via headers"); 
			} 
			if (!(obj instanceof ViaHeaderImpl))
			{
				throw new IllegalArgumentException("MessageFactory: Via Headers in bad format"); 
			} 
		}        

		ResponseImpl resImpl = new ResponseImpl();
        
        resImpl.setStatusCode(statusCode);
        resImpl.setCallIdHeader(callIdHeader);
        resImpl.setCSeqHeader(cSeqHeader);
        resImpl.setFromHeader(fromHeader);
        resImpl.setToHeader(toHeader);
        resImpl.setViaHeaders(viaHeaders);
        resImpl.setBody(body, contentTypeHeader);

        return resImpl;

    }

    /**
     * Creates Response without body based on specified Request
     * @param <var>statusCode</var> status code
     * @param <var>request</var> Request to base Response on
     * @throws IllegalArgumentException if request is null or
     * not from same JAIN SIP implementation
     * @throws SipParseException if statusCode is not accepted by implementation     
     */
    public Response createResponse(int statusCode, Request request)
        throws IllegalArgumentException, SipParseException
    {
		if(request == null)
		{
			throw new IllegalArgumentException("MessageFactory: Null Request");
		}
		if (!(request instanceof RequestImpl)) 
		{
			throw new IllegalArgumentException("request must be from" +
			"IBM JAIN SIP implementation");
		}
		RequestImpl requestImpl = (RequestImpl)request;
		
		ResponseImpl response = new ResponseImpl();
		
		response.setStatusCode(statusCode);
		
		CallIdHeader callId = request.getCallIdHeader();
		CSeqHeader cseq = request.getCSeqHeader();
		FromHeader from = request.getFromHeader();
		ToHeader to = request.getToHeader();
		if (callId == null || cseq == null || from == null || to == null) {
			throw new IllegalArgumentException("request lacks mandatory header");
		}
		response.setCallIdHeader((CallIdHeader)callId.clone());
		response.setCSeqHeader((CSeqHeader)cseq.clone());
		response.setFromHeader((FromHeader)from.clone());
		response.setToHeader((ToHeader)to.clone());
		
		ArrayList viaHeaders = new ArrayList(3);
		HeaderIterator iterator = request.getViaHeaders();
		if (iterator != null) 
		{
			while(iterator.hasNext())
			{
				viaHeaders.add(iterator.next());
			}
			if (!viaHeaders.isEmpty()) {
				response.setViaHeaders(viaHeaders);
			}
		}
		
		//According to RFC3261, Record-Route is only valid in 2xx,18x responses
		if ((statusCode >= 180 && statusCode < 190) || (statusCode >=200 && statusCode < 300) ){
			ArrayList recordRouteHeaders = new ArrayList(3);
			iterator = request.getRecordRouteHeaders();
			if (iterator != null) 
			{
				while(iterator.hasNext())
				{
					recordRouteHeaders.add(iterator.next());
				}
				if (!recordRouteHeaders.isEmpty()) {
					response.setRecordRouteHeaders(recordRouteHeaders);
				}
			}
		}

		if (requestImpl.isLoopback()) {
			response.setLoopback(true);
		}

        return response;
	}

    /**
     * Creates Response with body based on specified Request
     * @param <var>statusCode</var> status code
     * @param <var>request</var> Request to base Response on
     * @param <var>body</var> body of Request
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if body is null, or if request or
     * contentTypeHeader are null or not from same JAIN SIP implementation
     * @throws SipParseException if statusCode or body are not
     * accepted by implementation
     */
    public Response createResponse(int statusCode, Request request, byte[] body,
        ContentTypeHeader contentTypeHeader)
        throws IllegalArgumentException, SipParseException
    {
        ResponseImpl response =
					(ResponseImpl)createResponse(statusCode, request);
        
		
		ContentLengthHeader contentLength = 
			new HeaderFactoryImpl().createContentLengthHeader(body.length);
		
		response.setContentLengthHeader(contentLength);
		response.setBody(body, contentTypeHeader);

		return response;


    }

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
    public Response createResponse(int statusCode, Request request, String body,
        						    ContentTypeHeader contentTypeHeader) 
    	throws IllegalArgumentException, SipParseException
    {
        ResponseImpl response =
            (ResponseImpl)createResponse(statusCode, request);
        
        ContentLengthHeader contentLength = 
        	new HeaderFactoryImpl().createContentLengthHeader(body.length());
        
        response.setContentLengthHeader(contentLength);
        response.setBody(body, contentTypeHeader);
        
        return response;
	}

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
    public Response createResponse(int statusCode, CallIdHeader callIdHeader, 
    	CSeqHeader cSeqHeader, FromHeader fromHeader, ToHeader toHeader, 
    	List viaHeaders, byte[] body, ContentTypeHeader contentTypeHeader)
        throws IllegalArgumentException, SipParseException
    {

        if (callIdHeader == null
         || cSeqHeader   == null
         || fromHeader   == null
         || toHeader 	 == null
         || viaHeaders   == null
         || viaHeaders.isEmpty()
         || body 		 == null
         || contentTypeHeader == null)
        {
			throw new IllegalArgumentException("MessageFactory: null arguments");
        }
          
		// Check for null in via headers.
		for (int i = 0; i < viaHeaders.size(); i++)
		{
			Object obj = viaHeaders.get(i);
			if (obj == null)
			{   
				throw new IllegalArgumentException("Message Factory: Null via headers"); 
			} 
			if (!(obj instanceof ViaHeaderImpl))
			{
				throw new IllegalArgumentException("MessageFactory: Via Headers in bad format"); 
			} 
		}        


        ResponseImpl resImpl = new ResponseImpl();
        
        resImpl.setStatusCode(statusCode);
        resImpl.setCallIdHeader(callIdHeader);
        resImpl.setCSeqHeader(cSeqHeader);
        resImpl.setFromHeader(fromHeader);
        resImpl.setToHeader(toHeader);
        resImpl.setViaHeaders(viaHeaders);
        resImpl.setBody(body, contentTypeHeader);

        return resImpl;
    }

}
