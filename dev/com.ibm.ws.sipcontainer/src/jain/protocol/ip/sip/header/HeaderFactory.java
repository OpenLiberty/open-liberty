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
import jain.protocol.ip.sip.address.NameAddress;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;

/**
 * This interface provides factory methods to allow an application create Header
 * objects from a particular JAIN SIP implementation.
 *
 * @version 1.0
 */
public interface HeaderFactory
{
    
    /**
     * Creates an AcceptHeader based on the specified type and subType
     * @param <var>type</var> media type
     * @param <var>subType</var> media sub-type
     * @throws IllegalArgumentException if type or sub-type is null
     * @throws SipParseException if contentType or contentSubType is not accepted by implementation
     */
    public AcceptHeader createAcceptHeader(String contentType, String contentSubType)
                         throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates AcceptLanguageHeader based on given language-range
     * @param <var>languageRange</var> language-range
     * @throws IllegalArgumentException if languageRange is null
     * @throws SipParseException if languageRange is not accepted by implementation
     */
    public AcceptLanguageHeader createAcceptLanguageHeader(String languageRange)
                                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an AllowHeader based on given method
     * @param <var>method</var> method
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if method is not accepted by implementation
     */
    public AllowHeader createAllowHeader(String method)
                        throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a TimeStampHeader based on given timestamp
     * @param <var>timeStamp</var> time stamp
     * @throws SipParseException if timestamp is not accepted by implementation
     */
    public TimeStampHeader createTimeStampHeader(float timeStamp)
                            throws SipParseException;
    
    /**
     * Creates a ViaHeader based on given host
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public ViaHeader createViaHeader(String host)
                      throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a WarningHeader based on given code, agent and text
     * @param <var>code</var> code
     * @param <var>host</var> agent
     * @param <var>text</var> text
     * @throws IllegalArgumentException if agent or text are is null
     * @throws SipParseException if code, agent or text are not accepted by implementation
     */
    public WarningHeader createWarningHeader(int code, String agent, String text)
                          throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a RequireHeader based on given option tag
     * @param <var>optionTag</var> option tag
     * @throws IllegalArgumentException if optionTag is null
     * @throws SipParseException if optionTag is not accepted by implementation
     */
    public RequireHeader createRequireHeader(String optionTag)
                          throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a RetryAfterHeader based on given date string
     * @param <var>date</var> date string
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public RetryAfterHeader createRetryAfterHeader(String date)
                             throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an AuthorizationHeader based on given scheme
     * @param <var>scheme</var> authentication scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public AuthorizationHeader createAuthorizationHeader(String scheme)
                                throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ViaHeader based on given host and transport
     * @param <var>host</var> host
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if host or transport are null
     * @throws SipParseException if host or transport are not accepted by implementation
     */
    public ViaHeader createViaHeader(String host, String transport)
                      throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a CallIdHeader based on given Call-Id
     * @param <var>callId</var> call-id
     * @throws IllegalArgumentException if callId is null
     * @throws SipParseException if callId is not accepted by implementation
     */
    public CallIdHeader createCallIdHeader(String callId)
                         throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ProxyRequireHeader based on given option tag
     * @param <var>optionTag</var> option tag
     * @throws IllegalArgumentException if optionTag is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public ProxyRequireHeader createProxyRequireHeader(String optionTag)
                               throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ContactHeader based on given NameAddress
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public ContactHeader createContactHeader(NameAddress nameAddress)
                          throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a RetryAfterHeader based on given number of delta-seconds
     * @param <var>deltaSeconds</var> number of delta-seconds
     * @throws SipParseException if deltaSeconds is not accepted by implementation
     */
    public RetryAfterHeader createRetryAfterHeader(long deltaSeconds)
                             throws SipParseException;
    
    /**
     * Creates a wildcard ContactHeader. This is used in RegisterMessages
     * to indicate to the server that it should remove all locations the
     * at which the user is currently available
     */
    public ContactHeader createContactHeader();
    
    /**
     * Creates a ServerHeader based on given List of products
     * (Note that the Objects in the List must be Strings)
     * @param <var>products</var> products
     * @throws IllegalArgumentException if products is null, empty, or contains
     * any null elements, or contains any non-String objects
     * @throws SipParseException if any element of products is not accepted by implementation
     */
    public ServerHeader createServerHeader(List products)
                         throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ContentEncodingHeader based on given content-encoding
     * @param <var>contentEncoding</var> content-encoding
     * @throws IllegalArgumentException if contentEncoding is null
     * @throws SipParseException if contentEncoding is not accepted by implementation
     */
    public ContentEncodingHeader createContentEncodingHeader(String contentEncoding)
                                  throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a UnsupportedHeader based on given option tag
     * @param <var>optionTag</var> option tag
     * @throws IllegalArgumentException if optionTag is null
     * @throws SipParseException if optionTag is not accepted by implementation
     */
    public UnsupportedHeader createUnsupportedHeader(String optionTag)
                              throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ContentLengthHeader based on given content-length
     * @param <var>contentLength</var> content-length
     * @throws SipParseException if contentLength is not accepted by implementation
     */
    public ContentLengthHeader createContentLengthHeader(int contentLength)
                                throws SipParseException;
    
    /**
     * Creates a ViaHeader based on given host and port
     * @param <var>host</var> host
     * @param <var>port</var> port
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host or port is not accepted by implementation
     */
    public ViaHeader createViaHeader(int port, String host)
                      throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ContentTypeHeader based on given media type and sub-type
     * @param <var>type</var> media type
     * @param <var>subType</var> media sub-type
     * @throws IllegalArgumentException if type or subtype are null
     * @throws SipParseException if contentType or contentSubType is not accepted by implementation
     */
    public ContentTypeHeader createContentTypeHeader(String contentType, String contentSubType)
                              throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ViaHeader based on given host, port and transport
     * @param <var>host</var> host
     * @param <var>port</var> port
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if host or transport are null
     * @throws SipParseException if host, port or transport are not accepted by implementation
     */
    public ViaHeader createViaHeader(InetAddress host, int port, String transport)
                      throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a CSeqHeader based on given sequence number and method
     * @param <var>sequenceNumber</var> sequence number
     * @param <var>method</var> method
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if sequenceNumber or method are not accepted by implementation
     */
    public CSeqHeader createCSeqHeader(long sequenceNumber, String method)
                       throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an AcceptEncodingHeader with given content-encoding
     * @param <var>contentEncoding</var> the content-cenoding
     * @throws IllegalArgumentException if contentEncoding is null
     * @throws SipParseException if contentEncoding is not accepted by implementation
     */
    public AcceptEncodingHeader createAcceptEncodingHeader(String contentEncoding)
                                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a DateHeader based on given date string
     * @param <var>date</var> date string
     * @throws IllegalArgumentException if date string is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public DateHeader createDateHeader(String date)
                       throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a RecordRouteHeader based on given NameAddress
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public RecordRouteHeader createRecordRouteHeader(NameAddress nameAddress)
                              throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an DateHeader based on given Date
     * @param <var>date</var> Date
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public DateHeader createDateHeader(Date date)
                       throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ResponseKeyHeader based on given scheme
     * @param <var>scheme</var> scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public ResponseKeyHeader createResponseKeyHeader(String scheme)
                              throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an EncryptionHeader based on given scheme
     * @param <var>scheme</var> scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public EncryptionHeader createEncryptionHeader(String scheme)
                             throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a RetryAfterHeader based on given date
     * @param <var>date</var> date
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public RetryAfterHeader createRetryAfterHeader(Date date)
                             throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an ExpiresHeader based on given number of delta-seconds
     * @param <var>deltaSeconds</var> delta-seconds
     * @throws SipParseException if deltaSeconds is not accepted by implementation
     */
    public ExpiresHeader createExpiresHeader(long deltaSeconds)
                          throws SipParseException;
    
    /**
     * Creates a RouteHeader based on given NameAddresss
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public RouteHeader createRouteHeader(NameAddress nameAddress)
                        throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an ExpiresHeader based on given date
     * @param <var>date</var> date
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public ExpiresHeader createExpiresHeader(Date date)
                          throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a SubjectHeader based on given subject
     * @param <var>subject</var> subject
     * @throws IllegalArgumentException if subject is null
     * @throws SipParseException if subject is not accepted by implementation
     */
    public SubjectHeader createSubjectHeader(String subject)
                          throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an ExpiresHeader based on given date string
     * @param <var>date</var> date string
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public ExpiresHeader createExpiresHeader(String date)
                          throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ToHeader based on given NameAddress
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public ToHeader createToHeader(NameAddress nameAddress)
                     throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a FromHeader based on given NameAddress
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public FromHeader createFromHeader(NameAddress nameAddress)
                       throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a UserAgentHeader based on given List of products
     * (Note that the Objects in the List must be Strings)
     * @param <var>products</var> products
     * @throws IllegalArgumentException if products is null, empty, or contains
     * any null elements, or contains any non-String objects
     * @throws SipParseException if any element of products is not accepted by implementation
     */
    public UserAgentHeader createUserAgentHeader(List products)
                            throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a Header based on given token and value
     * @param <var>name</var> name
     * @param <var>value</var> value
     * @throws IllegalArgumentException if name or value are null
     * @throws SipParseException if name or value are not accepted by implementation
     */
    public Header createHeader(String name, String value)
                   throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ViaHeader based on given host
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public ViaHeader createViaHeader(InetAddress host)
                      throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a HideHeader based on hide value
     * @param <var>hide</var> hide value
     * @throws IllegalArgumentException if hide is null
     * @throws SipParseException if hide is not accepted by implementation
     */
    public HideHeader createHideHeader(String hide)
                       throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ViaHeader based on given host and port
     * @param <var>host</var> host
     * @param <var>port</var> port
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host or port is not accepted by implementation
     */
    public ViaHeader createViaHeader(int port, InetAddress host)
                      throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a MaxForwardsHeader based on given number of max-forwards
     * @param <var>maxForwards</var> number of max-forwards
     * @throws SipParseException if maxForwards is not accepted by implementation
     */
    public MaxForwardsHeader createMaxForwardsHeader(int maxForwards)
                              throws SipParseException;
    
    /**
     * Creates a ViaHeader based on given host and transport
     * @param <var>host</var> host
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if host or transport are null
     * @throws SipParseException if host or transport are not accepted by implementation
     */
    public ViaHeader createViaHeader(InetAddress host, String transport)
                      throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates an OrganizationHeader based on given organization
     * @param <var>organization</var> organization
     * @throws IllegalArgumentException if organization is null
     * @throws SipParseException if organization is not accepted by implementation
     */
    public OrganizationHeader createOrganizationHeader(String organization)
                               throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ViaHeader based on given host, port and transport
     * @param <var>host</var> host
     * @param <var>port</var> port
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if host or transport are null
     * @throws SipParseException if host, port or transport are not accepted by implementation
     */
    public ViaHeader createViaHeader(String host, int port, String transport)
                      throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a PriorityHeader based on given priority
     * @param <var>priority</var> priority
     * @throws IllegalArgumentException if priority is null
     * @throws SipParseException if priority is not accepted by implementation
     */
    public PriorityHeader createPriorityHeader(String priority)
                           throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a WWWAuthenticateHeader based on given scheme
     * @param <var>scheme</var> authentication scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public WWWAuthenticateHeader createWWWAuthenticateHeader(String scheme)
                                  throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ProxyAuthenticateHeader based on given scheme
     * @param <var>scheme</var> authentication scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public ProxyAuthenticateHeader createProxyAuthenticateHeader(String scheme)
                                    throws IllegalArgumentException,SipParseException;
    
    /**
     * Creates a ProxyAuthorizationHeader based on given scheme
     * @param <var>scheme</var> authentication scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public ProxyAuthorizationHeader createProxyAuthorizationHeader(String scheme)
                                     throws IllegalArgumentException,SipParseException;
}
