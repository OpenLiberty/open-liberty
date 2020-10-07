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
import jain.protocol.ip.sip.header.AuthorizationHeader;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.HideHeader;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.PriorityHeader;
import jain.protocol.ip.sip.header.ProxyAuthorizationHeader;
import jain.protocol.ip.sip.header.ResponseKeyHeader;
import jain.protocol.ip.sip.header.SubjectHeader;
import jain.protocol.ip.sip.header.ViaHeader;

import java.util.List;

/**
 * <p>
 * This interface represents a SIP Request i.e. a request from a client to a server.
 * </p>
 * There are six defined methods for a Request in the JAIN SIP API:
 * <LI>ACK - confirms that client has received a final
 * Response to an INVITE Request</LI>
 * <LI>BYE - indicates to the server that client
 * wishes to release the call leg</LI>
 * <LI>CANCEL - cancels a pending Request</LI>
 * <LI>INVITE - indicates that user or service is being invited
 * to participate in a session</LI>
 * <LI>OPTIONS - queries server as to its capabilities</LI>
 * <LI>REGISTER - register address with a SIP server</LI>
 *
 * Ceratin Requests may contain a body(entity) which contains a session description
 * in a format such as SDP (Session Description Protocol). Requests also contain
 * various Headers which describe the routing of the message and the message body.
 * </p>
 *
 * @version 1.0
 *
 */
public interface Request extends Message
{
    
    /**
     * Gets method of Request.
     * @return method of Request
     * @throws SipParseException if implementation cannot parse method
     */
    public String getMethod()
                   throws SipParseException;
    
    /**
     * Gets Request URI of Request.
     * @return Request URI of Request
     * @throws SipParseException if implementation cannot parse Request URI
     */
    public URI getRequestURI()
                throws SipParseException;
    
    /**
     * Sets RequestURI of Request.
     * @param <var>requestURI</var> Request URI to set
     * @throws IllegalArgumentException if requestURI is null or not from same
     * JAIN SIP implementation
     */
    public void setRequestURI(URI requestURI)
                 throws IllegalArgumentException;
    
    /**
     * Gets PriorityHeader of Request.
     * (Returns null if no PriorityHeader exists)
     * @return PriorityHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public PriorityHeader getPriorityHeader()
                           throws HeaderParseException;
    
    /**
     * Gets SubjectHeader of InviteMessage.
     * (Returns null if no SubjectHeader exists)
     * @return SubjectHeader of InviteMessage
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public SubjectHeader getSubjectHeader()
                          throws HeaderParseException;
    
    /**
     * Gets HeaderIterator of RequireHeaders of Request.
     * (Returns null if no RequireHeaders exist)
     * @return HeaderIterator of RequireHeaders of Request
     */
    public HeaderIterator getRequireHeaders();
    
    /**
     * Gets HeaderIterator of RouteHeaders of Request.
     * (Returns null if no RouteHeaders exist)
     * @return HeaderIterator of RouteHeaders of Request
     */
    public HeaderIterator getRouteHeaders();
    
    /**
     * Gets ResponseKeyHeader of Request.
     * (Returns null if no ResponseKeyHeader exists)
     * @return ResponseKeyHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public ResponseKeyHeader getResponseKeyHeader()
                              throws HeaderParseException;
    
    /**
     * Gets AuthorizationHeader of Request.
     * (Returns null if no AuthorizationHeader exists)
     * @return AuthorizationHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public AuthorizationHeader getAuthorizationHeader()
                                throws HeaderParseException;
    
    /**
     * Sets ProxyRequireHeaders of Request.
     * @param <var>proxyRequireHeaders</var> List of ProxyRequireHeaders to set
     * @throws IllegalArgumentException if proxyRequireHeaders is null, empty, contains
     * any elements that are null or not ProxyRequireHeaders from the same
     * JAIN SIP implementation
     */
    public void setProxyRequireHeaders(List proxyRequireHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Gets boolean value to indicate if Request
     * has AuthorizationHeader
     * @return boolean value to indicate if Request
     * has AuthorizationHeader
     */
    public boolean hasAuthorizationHeader();
    
    /**
     * Sets RequireHeaders of Request.
     * @param <var>requireHeaders</var> List of RequireHeaders to set
     * @throws IllegalArgumentException if requireHeaders is null, empty, contains
     * any elements that are null or not RequireHeaders from the same
     * JAIN SIP implementation
     */
    public void setRequireHeaders(List requireHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Sets AuthorizationHeader of Request.
     * @param <var>authorizationHeader</var> AuthorizationHeader to set
     * @throws IllegalArgumentException if authorizationHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setAuthorizationHeader(AuthorizationHeader authorizationHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets RouteHeaders of Request.
     * @param <var>routeHeaders</var> List of RouteHeaders to set
     * @throws IllegalArgumentException if routeHeaders is null, empty, contains
     * any elements that are null or not RouteHeaders from the same
     * JAIN SIP implementation
     */
    public void setRouteHeaders(List routeHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Removes AuthorizationHeader from Request (if it exists)
     */
    public void removeAuthorizationHeader();
    
    /**
     * Sets ResponseKeyHeader of Request.
     * @param <var>responseKeyHeader</var> ResponseKeyHeader to set
     * @throws IllegalArgumentException if responseKeyHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setResponseKeyHeader(ResponseKeyHeader responseKeyHeader)
                 throws IllegalArgumentException;
    
    /**
     * Gets HideHeader of Request.
     * (Returns null if no AuthorizationHeader exists)
     * @return HideHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public HideHeader getHideHeader()
                       throws HeaderParseException;
    
    /**
     * Sets PriorityHeader of Request.
     * @param <var>priorityHeader</var> PriorityHeader to set
     * @throws IllegalArgumentException if priorityHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setPriorityHeader(PriorityHeader priorityHeader)
                 throws IllegalArgumentException;
    
    /**
     * Gets boolean value to indicate if Request
     * has HideHeader
     * @return boolean value to indicate if Request
     * has HideHeader
     */
    public boolean hasHideHeader();
    
    /**
     * Sets SubjectHeader of InviteMessage.
     * @param <var>subjectHeader</var> SubjectHeader to set
     * @throws IllegalArgumentException if subjectHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setSubjectHeader(SubjectHeader subjectHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets HideHeader of Request.
     * @param <var>hideHeader</var> HideHeader to set
     * @throws IllegalArgumentException if hideHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setHideHeader(HideHeader hideHeader)
                 throws IllegalArgumentException;
    
    /**
     * Removes ProxyRequireHeaders from Request (if any exist)
     */
    public void removeProxyRequireHeaders();
    
    /**
     * Removes HideHeader from Request (if it exists)
     */
    public void removeHideHeader();
    
    /**
     * Adds ViaHeader to top of Request's ViaHeaders.
     * @param <var>viaHeader</var> ViaHeader to add
     * @throws IllegalArgumentException if viaHeader is null or not from same
     * JAIN SIP implementation
     */
    public void addViaHeader(ViaHeader viaHeader)
                 throws IllegalArgumentException;
    
    /**
     * Gets MaxForwardsHeader of Request.
     * (Returns null if no MaxForwardsHeader exists)
     * @return MaxForwardsHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public MaxForwardsHeader getMaxForwardsHeader()
                              throws HeaderParseException;
    
    /**
     * Removes RequireHeaders from Request (if any exist)
     */
    public void removeRequireHeaders();
    
    /**
     * Gets boolean value to indicate if Request
     * has MaxForwardsHeader
     * @return boolean value to indicate if Request
     * has MaxForwardsHeader
     */
    public boolean hasMaxForwardsHeader();
    
    /**
     * Gets boolean value to indicate if Request
     * has RouteHeaders
     * @return boolean value to indicate if Request
     * has RouteHeaders
     */
    public boolean hasRouteHeaders();
    
    /**
     * Sets MaxForwardsHeader of Request.
     * @param <var>maxForwardsHeader</var> MaxForwardsHeader to set
     * @throws IllegalArgumentException if maxForwardsHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setMaxForwardsHeader(MaxForwardsHeader maxForwardsHeader)
                 throws IllegalArgumentException;
    
    /**
     * Removes RouteHeaders from Request (if any exist)
     */
    public void removeRouteHeaders();
    
    /**
     * Removes MaxForwardsHeader from Request (if it exists)
     */
    public void removeMaxForwardsHeader();
    
    /**
     * Gets boolean value to indicate if Request
     * has ResponseKeyHeader
     * @return boolean value to indicate if Request
     * has ResponseKeyHeader
     */
    public boolean hasResponseKeyHeader();
    
    /**
     * Gets ProxyAuthorizationHeader of Request.
     * (Returns null if no ProxyAuthorizationHeader exists)
     * @return ProxyAuthorizationHeader of Request
     * @throws HeaderParseException if implementation cannot parse header value
     */
    public ProxyAuthorizationHeader getProxyAuthorizationHeader()
                                     throws HeaderParseException;
    
    /**
     * Removes ResponseKeyHeader from Request (if it exists)
     */
    public void removeResponseKeyHeader();
    
    /**
     * Gets boolean value to indicate if Request
     * has ProxyAuthorizationHeader
     * @return boolean value to indicate if Request
     * has ProxyAuthorizationHeader
     */
    public boolean hasProxyAuthorizationHeader();
    
    /**
     * Gets boolean value to indicate if Request
     * has PriorityHeader
     * @return boolean value to indicate if Request
     * has PriorityHeader
     */
    public boolean hasPriorityHeader();
    
    /**
     * Sets ProxyAuthorizationHeader of Request.
     * @param <var>proxyAuthorizationHeader</var> ProxyAuthorizationHeader to set
     * @throws IllegalArgumentException if proxyAuthorizationHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setProxyAuthorizationHeader(ProxyAuthorizationHeader proxyAuthorizationHeader)
                 throws IllegalArgumentException;
    
    /**
     * Removes PriorityHeader from Request (if it exists)
     */
    public void removePriorityHeader();
    
    /**
     * Removes ProxyAuthorizationHeader from Request (if it exists)
     */
    public void removeProxyAuthorizationHeader();
    
    /**
     * Gets boolean value to indicate if Request
     * has SubjectHeader
     * @return boolean value to indicate if Request
     * has SubjectHeader
     */
    public boolean hasSubjectHeader();
    
    /**
     * Gets HeaderIterator of ProxyRequireHeaders of Request.
     * (Returns null if no ProxyRequireHeaders exist)
     * @return HeaderIterator of ProxyRequireHeaders of Request
     */
    public HeaderIterator getProxyRequireHeaders();
    
    /**
     * Removes SubjectHeader from Request (if it exists)
     */
    public void removeSubjectHeader();
    
    /**
     * Gets boolean value to indicate if Request
     * has ProxyRequireHeaders
     * @return boolean value to indicate if Request
     * has ProxyRequireHeaders
     */
    public boolean hasProxyRequireHeaders();
    
    /**
     * Gets boolean value to indicate if Request
     * has RequireHeaders
     * @return boolean value to indicate if Request
     * has RequireHeaders
     */
    public boolean hasRequireHeaders();
    
    /**
     * ACK method constant
     */
    public static final String ACK = "ACK";
    
    /**
     * BYE method constant
     */
    public static final String BYE = "BYE";
    
    /**
     * CANCEL method constant
     */
    public static final String CANCEL = "CANCEL";
    
    /**
     * OPTIONS method constant
     */
    public static final String OPTIONS = "OPTIONS";
    
    /**
     * REGISTER method constant
     */
    public static final String REGISTER = "REGISTER";
    
    /**
     * INVITE method constant
     */
    public static final String INVITE = "INVITE";
    /**
     * ILLEGAL method constant
     */
    public static final String ILLEGAL = "ILLEGAL";
}
