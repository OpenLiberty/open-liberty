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
import jain.protocol.ip.sip.header.AuthorizationHeader;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.HideHeader;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.PriorityHeader;
import jain.protocol.ip.sip.header.ProxyAuthorizationHeader;
import jain.protocol.ip.sip.header.ProxyRequireHeader;
import jain.protocol.ip.sip.header.RequireHeader;
import jain.protocol.ip.sip.header.ResponseKeyHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.SubjectHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.message.Request;

import java.util.List;

import com.ibm.ws.jain.protocol.ip.sip.address.URIImpl;
import com.ibm.ws.sip.parser.util.CharsBuffer;

/**
 * Request message implementation.
 * @author Assaf Azaria, April 2003.                
 */
public class RequestImpl extends MessageImpl implements Request
{
    /** Serialization UID (do not change) */
    private static final long serialVersionUID = 2735560974454675635L;

	/**
	 * String constant for the INFO method 
	 */
    public static final String INFO = "INFO";
	/**
	 * String constant for the PRACK method 
	 */
	public static final String PRACK = "PRACK";
	/**
	 * String constant for the SUBSCRIBE method 
	 */
	public static final String SUBSCRIBE = "SUBSCRIBE";
	/**
	 * String constant for the NOTIFY method 
	 */
	public static final String NOTIFY = "NOTIFY";
	/**
	 * String constant for the PUBLISH method 
	 */
	public static final String PUBLISH = "PUBLISH";
	/**
	 * String constant for the MESSAGE method 
	 */
	public static final String MESSAGE = "MESSAGE";
	/**
	 * String constant for the REFER method 
	 */
	public static final String REFER = "REFER";
	/**
	 * String constant for the UPDATE method 
	 */
	public static final String UPDATE = "UPDATE";
	/**
	 * String constant for the KEEPALIVE method 
	 */
	public static final String KEEPALIVE = "KEEPALIVE";
	/**
	 * String constant for the PROXYERROR method 
	 */
	public static final String PROXYERROR = "PROXYERROR";
	
    //
    // Members.
    //

    /**
     * The request line.
     */
    RequestLine m_requestLine = new RequestLine();

	//
	// Constructors.
	//
	
    /** 
     * Construct a new RequestImpl object.
     */
    public RequestImpl()
    {
    }

    
    //
    // Operations.
    //

    /**
     * Adds ViaHeader to top of Request's ViaHeaders.
     * @param <var>viaHeader</var> ViaHeader to add
     * @throws IllegalArgumentException if viaHeader is null or not from same
     * JAIN SIP implementation
    */
    public void addViaHeader(ViaHeader viaHeader)
        throws IllegalArgumentException
    {
		addHeader(viaHeader, true);
    }

    /**
     * Gets method of Request.
     * @return method of Request
     * @throws SipParseException if implementation cannot parse method
    */
    public String getMethod() throws SipParseException
    {
        return m_requestLine.getMethod();
    }

    /**
     * Sets method of Request.
     * @param <var>method</var> method set
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if implementation cannot parse method
    */
    public void setMethod(String method)
        throws IllegalArgumentException, SipParseException
    {
        if (method == null)
        {
        	throw new IllegalArgumentException("Request: null method");
        }
        if (method.length() == 0)
        {
        	throw new SipParseException("Request: Empty method", "");
        }
        
        m_requestLine.setMethod(method);
    }

    /**
     * Gets Request URI of Request.
     * @return Request URI of Request
     * @throws SipParseException if implementation cannot parse Request URI
     */
    public URI getRequestURI() throws SipParseException
    {
        return m_requestLine.getURI();
    }

    /**
     * Sets RequestURI of Request.
     * @param <var>requestURI</var> Request URI to set
     * @throws IllegalArgumentException if requestURI is null or not from same
     * JAIN SIP implementation
    */
    public void setRequestURI(URI requestURI) throws IllegalArgumentException
    {
		if(requestURI == null)
		{
			throw new IllegalArgumentException("Request: Null requestURI");
		}
		if(!(requestURI instanceof URIImpl))
		{
			throw new IllegalArgumentException("Request: requestURI must be" +
			"from IBM Jain SIP implementation");
		}
		
		m_requestLine.setURI(requestURI);
	}

    /**
     * Gets AuthorizationHeader of Request.
     * (Returns null if no AuthorizationHeader exists)
     * @return AuthorizationHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public AuthorizationHeader getAuthorizationHeader()
        throws HeaderParseException
    {
        return (AuthorizationHeader)getHeader(AuthorizationHeader.name, true);
    }

    /**
     * Gets boolean value to indicate if Request
     * has AuthorizationHeader
     * @return boolean value to indicate if Request
     * has AuthorizationHeader
    */
    public boolean hasAuthorizationHeader()
    {
        return hasHeaders(AuthorizationHeader.name);
    }

    /**
     * Sets AuthorizationHeader of Request.
     * @param <var>authorizationHeader</var> AuthorizationHeader to set
     * @throws IllegalArgumentException if authorizationHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setAuthorizationHeader(AuthorizationHeader authorizationHeader)
        throws IllegalArgumentException
    {
		setHeader(authorizationHeader, true);
    }

    /**
     * Removes AuthorizationHeader from Request (if it exists)
     */
    public void removeAuthorizationHeader()
    {
		removeHeaders(AuthorizationHeader.name);
    }

    /**
     * Gets HideHeader of Request.
     * (Returns null if no AuthorizationHeader exists)
     * @return HideHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public HideHeader getHideHeader() throws HeaderParseException
    {
		return (HideHeader)getHeader(HideHeader.name, true);
    }

    /**
     * Gets boolean value to indicate if Request
     * has HideHeader
     * @return boolean value to indicate if Request
     * has HideHeader
     */
    public boolean hasHideHeader()
    {
		return hasHeaders(HideHeader.name);
    }

    /**
     * Sets HideHeader of Request.
     * @param <var>hideHeader</var> HideHeader to set
     * @throws IllegalArgumentException if hideHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setHideHeader(HideHeader hideHeader)
        throws IllegalArgumentException
    {
		setHeader(hideHeader, true);
    }

    /**
     * Removes HideHeader from Request (if it exists)
     */
    public void removeHideHeader()
    {
		removeHeaders(HideHeader.name);
    }

    /**
     * Gets MaxForwardsHeader of Request.
     * (Returns null if no MaxForwardsHeader exists)
     * @return MaxForwardsHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public MaxForwardsHeader getMaxForwardsHeader() throws HeaderParseException
    {
		return (MaxForwardsHeader)getHeader(MaxForwardsHeader.name, true);
    }

    /**
     * Gets boolean value to indicate if Request
     * has MaxForwardsHeader
     * @return boolean value to indicate if Request
     * has MaxForwardsHeader
     */
    public boolean hasMaxForwardsHeader()
    {
		return hasHeaders(MaxForwardsHeader.name);
    }

    /**
     * Sets MaxForwardsHeader of Request.
     * @param <var>maxForwardsHeader</var> MaxForwardsHeader to set
     * @throws IllegalArgumentException if maxForwardsHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setMaxForwardsHeader(MaxForwardsHeader maxForwardsHeader)
        throws IllegalArgumentException
    {
		setHeader(maxForwardsHeader, true);
    }

    /**
     * Removes MaxForwardsHeader from Request (if it exists)
     */
    public void removeMaxForwardsHeader()
    {
		removeHeaders(MaxForwardsHeader.name);
    }

    /**
     * Gets ProxyAuthorizationHeader of Request.
     * (Returns null if no ProxyAuthorizationHeader exists)
     * @return ProxyAuthorizationHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public ProxyAuthorizationHeader getProxyAuthorizationHeader()
        throws HeaderParseException
    {
		return (ProxyAuthorizationHeader)getHeader(ProxyAuthorizationHeader.name, true);
    }

    /**
     * Gets boolean value to indicate if Request
     * has ProxyAuthorizationHeader
     * @return boolean value to indicate if Request
     * has ProxyAuthorizationHeader
     */
    public boolean hasProxyAuthorizationHeader()
    {
		return hasHeaders(ProxyAuthorizationHeader.name);
    }

    /**
     * Sets ProxyAuthorizationHeader of Request.
     * @param <var>proxyAuthorizationHeader</var> ProxyAuthorizationHeader 
     * to set
     * @throws IllegalArgumentException if proxyAuthorizationHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setProxyAuthorizationHeader(ProxyAuthorizationHeader proxyAuthorizationHeader)
        throws IllegalArgumentException
    {
		setHeader(proxyAuthorizationHeader, true);
    }

    /**
     * Removes ProxyAuthorizationHeader from Request (if it exists)
     */
    public void removeProxyAuthorizationHeader()
    {
		removeHeaders(ProxyAuthorizationHeader.name);
    }

    /**
     * Gets HeaderIterator of ProxyRequireHeaders of Request.
     * (Returns null if no ProxyRequireHeaders exist)
     * @return HeaderIterator of ProxyRequireHeaders of Request
     */
    public HeaderIterator getProxyRequireHeaders()
    {
		return getHeaders(ProxyRequireHeader.name);
    }

    /**
     * Gets boolean value to indicate if Request
     * has ProxyRequireHeaders
     * @return boolean value to indicate if Request
     * has ProxyRequireHeaders
     */
    public boolean hasProxyRequireHeaders()
    {
		return hasHeaders(ProxyRequireHeader.name);
    }

    /**
     * Sets ProxyRequireHeaders of Request.
     * @param <var>proxyRequireHeaders</var> List of ProxyRequireHeaders to set
     * @throws IllegalArgumentException if proxyRequireHeaders is null, empty,
     * contains any elements that are null or not ProxyRequireHeaders from the 
     * same JAIN SIP implementation
     */
    public void setProxyRequireHeaders(List proxyRequireHeaders)
        throws IllegalArgumentException
    {
		setHeaders(ProxyRequireHeader.name, proxyRequireHeaders);
    }

    /**
     * Removes ProxyRequireHeaders from Request (if any exist)
     */
    public void removeProxyRequireHeaders()
    {
		removeHeaders(ProxyRequireHeader.name);
    }

    /**
     * Gets HeaderIterator of RequireHeaders of Request.
     * (Returns null if no RequireHeaders exist)
     * @return HeaderIterator of RequireHeaders of Request
     */
    public HeaderIterator getRequireHeaders()
    {
		return getHeaders(RequireHeader.name);
    }

    /**
     * Gets boolean value to indicate if Request
     * has RequireHeaders
     * @return boolean value to indicate if Request
     * has RequireHeaders
     */
    public boolean hasRequireHeaders()
    {
		return hasHeaders(RequireHeader.name);
    }

    /**
     * Sets RequireHeaders of Request.
     * @param <var>requireHeaders</var> List of RequireHeaders to set
     * @throws IllegalArgumentException if requireHeaders is null, empty, 
     * contains any elements that are null or not RequireHeaders from the same
     * JAIN SIP implementation
     */
    public void setRequireHeaders(List requireHeaders)
        throws IllegalArgumentException
    {
		setHeaders(RequireHeader.name, requireHeaders);
	}

    /**
     * Removes RequireHeaders from Request (if any exist)
     */
    public void removeRequireHeaders()
    {
		removeHeaders(RequireHeader.name);
    }

    /**
     * Gets HeaderIterator of RouteHeaders of Request.
     * (Returns null if no RouteHeaders exist)
     * @return HeaderIterator of RouteHeaders of Request
     */
    public HeaderIterator getRouteHeaders()
    {
		return getHeaders(RouteHeader.name);
    }

    /**
     * Gets boolean value to indicate if Request
     * has RouteHeaders
     * @return boolean value to indicate if Request
     * has RouteHeaders
     */
    public boolean hasRouteHeaders()
    {
		return hasHeaders(RouteHeader.name);
    }

    /**
     * Sets RouteHeaders of Request.
     * @param <var>routeHeaders</var> List of RouteHeaders to set
     * @throws IllegalArgumentException if routeHeaders is null, empty, contains
     * any elements that are null or not RouteHeaders from the same
     * JAIN SIP implementation
     */
    public void setRouteHeaders(List routeHeaders)
        throws IllegalArgumentException
    {
		setHeaders(RouteHeader.name, routeHeaders);
    }

    /**
     * Removes RouteHeaders from Request (if any exist)
     */
    public void removeRouteHeaders()
    {
		removeHeaders(RouteHeader.name);
    }

    /**
     * Gets ResponseKeyHeader of Request.
     * (Returns null if no ResponseKeyHeader exists)
     * @return ResponseKeyHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public ResponseKeyHeader getResponseKeyHeader() throws HeaderParseException
    {
    	return (ResponseKeyHeader)getHeader(ResponseKeyHeader.name, true);    
    }

    /**
     * Gets boolean value to indicate if Request
     * has ResponseKeyHeader
     * @return boolean value to indicate if Request
     * has ResponseKeyHeader
     */
    public boolean hasResponseKeyHeader()
    {
		return hasHeaders(ResponseKeyHeader.name);
    }

    /**
     * Sets ResponseKeyHeader of Request.
     * @param <var>responseKeyHeader</var> ResponseKeyHeader to set
     * @throws IllegalArgumentException if responseKeyHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setResponseKeyHeader(ResponseKeyHeader responseKeyHeader)
        throws IllegalArgumentException
    {
		setHeader(responseKeyHeader, true);
    }

    /**
     * Removes ResponseKeyHeader from Request (if it exists)
     */
    public void removeResponseKeyHeader()
    {
		removeHeaders(ResponseKeyHeader.name);
    }

    /**
     * Gets PriorityHeader of Request.
     * (Returns null if no PriorityHeader exists)
     * @return PriorityHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public PriorityHeader getPriorityHeader() throws HeaderParseException
    {
		return (PriorityHeader)getHeader(PriorityHeader.name, true);
    }

    /**
     * Gets boolean value to indicate if Request
     * has PriorityHeader
     * @return boolean value to indicate if Request
     * has PriorityHeader
     */
    public boolean hasPriorityHeader()
    {
		return hasHeaders(PriorityHeader.name);
    }

    /**
     * Sets PriorityHeader of Request.
     * @param <var>priorityHeader</var> PriorityHeader to set
     * @throws IllegalArgumentException if priorityHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setPriorityHeader(PriorityHeader priorityHeader)
        throws IllegalArgumentException
    {
		setHeader(priorityHeader, true);
    }

    /**
     * Removes PriorityHeader from Request (if it exists)
     */
    public void removePriorityHeader()
    {
		removeHeaders(PriorityHeader.name);
    }

    /**
     * Gets SubjectHeader of InviteMessage.
     * (Returns null if no SubjectHeader exists)
     * @return SubjectHeader of InviteMessage
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public SubjectHeader getSubjectHeader() throws HeaderParseException
    {
		return (SubjectHeader)getHeader(SubjectHeader.name, true);
    }

    /**
     * Gets boolean value to indicate if Request
     * has SubjectHeader
     * @return boolean value to indicate if Request
     * has SubjectHeader
     */
    public boolean hasSubjectHeader()
    {
		return hasHeaders(SubjectHeader.name);
    }

    /**
     * Sets SubjectHeader of InviteMessage.
     * @param <var>subjectHeader</var> SubjectHeader to set
     * @throws IllegalArgumentException if subjectHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setSubjectHeader(SubjectHeader subjectHeader)
        throws IllegalArgumentException
    {
		setHeader(subjectHeader, true);
    }

    /**
     * Removes SubjectHeader from Request (if it exists)
     */
    public void removeSubjectHeader()
    {
        removeHeaders(SubjectHeader.name);
    }
    
	/**
	 * Gets version major of Message.
	 * @return version major of Message
	 * @throws SipParseException if implementation could not parse 
	 *   version major
	 */
	public int getVersionMajor() throws SipParseException
	{
		String major = m_requestLine.getSipVersion().getVersionMajor();
		try
		{
			int ret = Integer.parseInt(major);
			return ret;
		}
		catch (NumberFormatException ex)
		{
			throw new SipParseException(ex.getMessage());
		}
	}

	/**
	 * Sets version of Message. Note that the version defaults to 2.0.
	 * (i.e. version major of 2 and version minor of 0)
	 * @param <var>versionMajor</var> version major
	 * @param <var>versionMinor</var> version minor
	 * @throws SipParseException if versionMajor or versionMinor are not 
	 * accepted by implementation
	 */
	public void setVersion(int versionMajor, int versionMinor)
		throws SipParseException
	{
		super.setVersion(versionMajor, versionMinor);
		
		m_requestLine.setSipVersion(getVersion());
	}

	/**
	 * Returns boolean value to indicate if Message is a Request.
	 * @return boolean value to indicate if Message is a Request
	 */
	public boolean isRequest()
	{
		return true;
	}
	
	/**
	 * Returns start line of Message
	 * @return start line of Message
	 */
	public String getStartLine()
	{
		return m_requestLine.toString();
	}
	
	/**
     * @see com.ibm.ws.jain.protocol.ip.sip.message.MessageImpl
     * #writeStartLineToBuffer(com.ibm.ws.jain.protocol.ip.sip.message.CharsBuffer)
     */
    public void writeStartLineToBuffer(CharsBuffer buffer, boolean network) 
    {
        m_requestLine.writeToCharBuffer(buffer, network);
    }

	
	/**
	 * Internal: set the status line.
	 */
	public void setRequestLine(RequestLine requestLine)
	{
		m_requestLine = requestLine;
	}

	/**
	 * Get the hash code for this object.
	 */
	public int hashCode()
	{
		return toString().hashCode();
	}

	/**
	 * Gets string representation of Message
	 * @return string representation of Message
	 */
	public String toString()
	{
		return super.toString();
	}


	/**
	 * Returns the originInviteTransaction.
	 * used only in Cancel Request
	 * @return long - the invite transaction Id  , -1 if the invite transaction was not found
	 */
	public long getOriginInviteTransaction() 
	{
		return -1;
	}
}
