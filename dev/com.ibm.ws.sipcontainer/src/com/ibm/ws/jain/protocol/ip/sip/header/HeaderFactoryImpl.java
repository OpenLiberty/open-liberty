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
package com.ibm.ws.jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.address.NameAddress;
import jain.protocol.ip.sip.header.AcceptEncodingHeader;
import jain.protocol.ip.sip.header.AcceptHeader;
import jain.protocol.ip.sip.header.AcceptLanguageHeader;
import jain.protocol.ip.sip.header.AllowHeader;
import jain.protocol.ip.sip.header.AuthorizationHeader;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContactHeader;
import jain.protocol.ip.sip.header.ContentEncodingHeader;
import jain.protocol.ip.sip.header.ContentLengthHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.DateHeader;
import jain.protocol.ip.sip.header.EncryptionHeader;
import jain.protocol.ip.sip.header.ExpiresHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderFactory;
import jain.protocol.ip.sip.header.HideHeader;
import jain.protocol.ip.sip.header.MaxForwardsHeader;
import jain.protocol.ip.sip.header.OrganizationHeader;
import jain.protocol.ip.sip.header.PriorityHeader;
import jain.protocol.ip.sip.header.ProxyAuthenticateHeader;
import jain.protocol.ip.sip.header.ProxyAuthorizationHeader;
import jain.protocol.ip.sip.header.ProxyRequireHeader;
import jain.protocol.ip.sip.header.RecordRouteHeader;
import jain.protocol.ip.sip.header.RequireHeader;
import jain.protocol.ip.sip.header.ResponseKeyHeader;
import jain.protocol.ip.sip.header.RetryAfterHeader;
import jain.protocol.ip.sip.header.RouteHeader;
import jain.protocol.ip.sip.header.ServerHeader;
import jain.protocol.ip.sip.header.SubjectHeader;
import jain.protocol.ip.sip.header.TimeStampHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.header.UnsupportedHeader;
import jain.protocol.ip.sip.header.UserAgentHeader;
import jain.protocol.ip.sip.header.ViaHeader;
import jain.protocol.ip.sip.header.WWWAuthenticateHeader;
import jain.protocol.ip.sip.header.WarningHeader;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.parser.HeaderCreator;
import com.ibm.ws.sip.parser.util.InetAddressCache;

/**
 * A factory for sip headers. 
 * 
 * @author  Assaf Azaria, Mar 2003.
 */
public class HeaderFactoryImpl implements HeaderFactory
{
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(HeaderFactoryImpl.class);
	
    /**
     * Creates an AcceptHeader based on the specified type and subType
     * @param <var>type</var> media type
     * @param <var>subType</var> media sub-type
     * @throws IllegalArgumentException if type or sub-type is null
     * @throws SipParseException if contentType or contentSubType is not accepted by implementation
     */
    public AcceptHeader createAcceptHeader(String contentType, String contentSubType)
        throws IllegalArgumentException, SipParseException
    {
        if (contentType == null)
        {
            throw new IllegalArgumentException("Null ContentType");
        }
        if (contentSubType == null)
        {
            throw new IllegalArgumentException("Null ContentSubType");
        }
        
        AcceptHeaderImpl acceptImpl = new AcceptHeaderImpl();
        acceptImpl.setContentType(contentType);
        acceptImpl.setContentSubType(contentSubType);
        return acceptImpl;
    }
    /**
     * Creates AcceptLanguageHeader based on given language-range
     * @param <var>languageRange</var> language-range
     * @throws IllegalArgumentException if languageRange is null
     * @throws SipParseException if languageRange is not accepted by implementation
     */
    public AcceptLanguageHeader createAcceptLanguageHeader(String languageRange)
        throws IllegalArgumentException, SipParseException
    {
        if (languageRange == null)
        {
            throw new IllegalArgumentException("Null LanguageRange");
        }
        
        AcceptLanguageHeaderImpl acceptLangImpl =
            new AcceptLanguageHeaderImpl();
        acceptLangImpl.setLanguageRange(languageRange);
        return acceptLangImpl;
    }
    /**
     * Creates an AllowHeader based on given method
     * @param <var>method</var> method
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if method is not accepted by implementation
     */
    public AllowHeader createAllowHeader(String method)
        throws IllegalArgumentException, SipParseException
    {
        if (method == null)
        {
            throw new IllegalArgumentException("Null Method");
        }
        
        AllowHeaderImpl allowImpl = new AllowHeaderImpl();
        allowImpl.setMethod(method);
        return allowImpl;
    }
    /**
     * Creates a TimeStampHeader based on given timestamp
     * @param <var>timeStamp</var> time stamp
     * @throws SipParseException if timestamp is not accepted by implementation
     */
    public TimeStampHeader createTimeStampHeader(float timeStamp)
        throws SipParseException
    {
        TimeStampHeaderImpl timeStampImpl = new TimeStampHeaderImpl();
        timeStampImpl.setTimeStamp(timeStamp);
        return timeStampImpl;
    }
    /**
     * Creates a ViaHeader based on given host.
     *
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public ViaHeader createViaHeader(String host)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {
            throw new IllegalArgumentException("Null Host");
        }
        
        ViaHeaderImpl viaImpl = new ViaHeaderImpl();
        viaImpl.setHost(host);
        return viaImpl;
    }
    /**
     * Creates a WarningHeader based on given code, agent and text.
     *
     * @param <var>code</var> code
     * @param <var>host</var> agent
     * @param <var>text</var> text
     * @throws IllegalArgumentException if agent or text are is null
     * @throws SipParseException if code, agent or text are not accepted by
     * implementation
     */
    public WarningHeader createWarningHeader(int code, String agent, 
    										  String text)
        throws IllegalArgumentException, SipParseException
    {
        if (agent == null)
        {
            throw new IllegalArgumentException("Null agent");
        }
        if (text == null)
        {
            throw new IllegalArgumentException("Null text");
        }
        
        WarningHeaderImpl warningImpl = new WarningHeaderImpl();
        warningImpl.setText(text);
        warningImpl.setCode(code);
        warningImpl.setAgent(agent);
        return warningImpl;
    }
    /**
     * Creates a RequireHeader based on given option tag
     * @param <var>optionTag</var> option tag
     * @throws IllegalArgumentException if optionTag is null
     * @throws SipParseException if optionTag is not accepted by implementation
     */
    public RequireHeader createRequireHeader(String optionTag)
        throws IllegalArgumentException, SipParseException
    {
        if (optionTag == null)
        {
            throw new IllegalArgumentException("Null optionTag");
        }
        
        RequireHeaderImpl requireImpl = new RequireHeaderImpl();
        requireImpl.setOptionTag(optionTag);
        return requireImpl;
    }
    /**
     * Creates a RetryAfterHeader based on given date string
     * @param <var>date</var> date string
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public RetryAfterHeader createRetryAfterHeader(String date)
        throws IllegalArgumentException, SipParseException
    {
        if (date == null)
        {
            throw new IllegalArgumentException("Null date");
        }
        
        RetryAfterHeaderImpl retryAfterImpl = new RetryAfterHeaderImpl();
        retryAfterImpl.setDate(date);
        return retryAfterImpl;
    }
    /**
     * Creates an AuthorizationHeader based on given scheme
     * @param <var>scheme</var> authentication scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public AuthorizationHeader createAuthorizationHeader(String scheme)
        throws IllegalArgumentException, SipParseException
    {
        if (scheme == null)
        {
            throw new IllegalArgumentException("Null scheme");
        }
        
        AuthorizationHeaderImpl authImpl = new AuthorizationHeaderImpl();
		authImpl.setScheme(scheme);
        return authImpl;
    }
    /**
     * Creates a ViaHeader based on given host and transport
     * @param <var>host</var> host
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if host or transport are null
     * @throws SipParseException if host or transport are not accepted by 
     * implementation
     */
    public ViaHeader createViaHeader(String host, String transport)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {
        	throw new IllegalArgumentException("Null Host");
        }
        	    
        if (transport == null)
        {
        	throw new IllegalArgumentException("Null Transport"); 
        } 
        
        ViaHeaderImpl viaImpl = new ViaHeaderImpl();
		viaImpl.setHost(host);
		viaImpl.setTransport(transport);
        return viaImpl;
    }
    
    /**
     * Creates a CallIdHeader based on given Call-Id
     * @param <var>callId</var> call-id
     * @throws IllegalArgumentException if callId is null
     * @throws SipParseException if callId is not accepted by implementation
     */
    public CallIdHeader createCallIdHeader(String callId)
        throws IllegalArgumentException, SipParseException
    {
        if (callId == null)
        {
        	throw new IllegalArgumentException("Null callId"); 
        } 
        
        CallIdHeaderImpl callIdImpl = new CallIdHeaderImpl();
		callIdImpl.setCallId(callId);
        return callIdImpl;
    }
    
    /**
     * Creates a ProxyRequireHeader based on given option tag
     * @param <var>optionTag</var> option tag
     * @throws IllegalArgumentException if optionTag is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public ProxyRequireHeader createProxyRequireHeader(String optionTag)
        throws IllegalArgumentException, SipParseException
    {
        if (optionTag == null)
        {
        	throw new IllegalArgumentException("Null optionTag is null"); 
        } 
        
        ProxyRequireHeaderImpl proxyImpl = new ProxyRequireHeaderImpl();
        proxyImpl.setOptionTag(optionTag);
        return proxyImpl;
    }
    
    /**
     * Creates a ContactHeader based on given NameAddress
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public ContactHeader createContactHeader(NameAddress nameAddress)
        throws IllegalArgumentException, SipParseException
    {
        if (nameAddress == null)
        {
        	throw new IllegalArgumentException("nameAddress is null "); 
        } 
        
        ContactHeaderImpl contactImpl = new ContactHeaderImpl();
        contactImpl.setNameAddress(nameAddress);
        return contactImpl;
    }
    /**
     * Creates a RetryAfterHeader based on given number of delta-seconds
     * @param <var>deltaSeconds</var> number of delta-seconds
     * @throws SipParseException if deltaSeconds is not accepted by 
     * implementation
     */
    public RetryAfterHeader createRetryAfterHeader(long deltaSeconds)
        throws SipParseException
    {
        RetryAfterHeaderImpl retryImpl = new RetryAfterHeaderImpl();
        retryImpl.setDeltaSeconds(deltaSeconds);
        return retryImpl;
    }
    /**
     * Creates a wildcard ContactHeader. This is used in RegisterMessages
     * to indicate to the server that it should remove all locations the
     * at which the user is currently available
     * @throws SipParseException
     */
    public ContactHeader createContactHeader() {
        ContactHeaderImpl contactImpl = new ContactHeaderImpl();
        
        // ASSAF: CHECK THIS.
        /* Bug fix -- bug reported by Maria Yndefors (e-horizon).
        Contact contact = (Contact)contactImpl.getImplementationObject();
        contact.setWildCardFlag(true);*/ 
        return contactImpl;
    }
    
    /**
     * Creates a ServerHeader based on given List of products
     * (Note that the Objects in the List must be Strings)
     * @param <var>products</var> products
     * @throws IllegalArgumentException if products is null, empty, or contains
     * any null elements, or contains any non-String objects
     * @throws SipParseException if any element of products is not accepted by
     * implementation
     */
    public ServerHeader createServerHeader(List products)
        throws IllegalArgumentException, SipParseException
    {
        ServerHeaderImpl serverImpl = new ServerHeaderImpl();
        serverImpl.setProducts(products);
        return serverImpl;
    }
    /**
     * Creates a ContentEncodingHeader based on given content-encoding
     * @param <var>contentEncoding</var> content-encoding
     * @throws IllegalArgumentException if contentEncoding is null
     * @throws SipParseException if contentEncoding is not accepted by 
     * implementation
     */
    public ContentEncodingHeader createContentEncodingHeader(String contentEncoding)
        throws IllegalArgumentException, SipParseException
    {
        if (contentEncoding == null)
        {
        	throw new IllegalArgumentException("Null ContentEncoding"); 
        } 
        
        ContentEncodingHeaderImpl contentEncodingImpl =
            new ContentEncodingHeaderImpl();
        contentEncodingImpl.setEncoding(contentEncoding);
        return contentEncodingImpl;
    }
    
    /**
     * Creates a UnsupportedHeader based on given option tag
     * @param <var>optionTag</var> option tag
     * @throws IllegalArgumentException if optionTag is null
     * @throws SipParseException if optionTag is not accepted by implementation
     */
    public UnsupportedHeader createUnsupportedHeader(String optionTag)
        throws IllegalArgumentException, SipParseException
    {
        if (optionTag == null)
        {    
        	throw new IllegalArgumentException("Null optionTag"); 
        } 
        
        UnsupportedHeaderImpl unsupportedImpl = new UnsupportedHeaderImpl();
        unsupportedImpl.setOptionTag(optionTag);
        return unsupportedImpl;
    }
    
    /**
     * Creates a ContentLengthHeader based on given content-length
     * @param <var>contentLength</var> content-length
     * @throws SipParseException if contentLength is not accepted by 
     * implementation
     */
    public ContentLengthHeader createContentLengthHeader(int contentLength)
        throws SipParseException
    {
        ContentLengthHeaderImpl contentLengthImpl =
            new ContentLengthHeaderImpl();
        contentLengthImpl.setContentLength(contentLength);
        return contentLengthImpl;
    }
    
    /**
     * Creates a ViaHeader based on given host and port
     * @param <var>host</var> host
     * @param <var>port</var> port
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host or port is not accepted by 
     * implementation
     */
    public ViaHeader createViaHeader(int port, String host)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {
			throw new IllegalArgumentException("Null host");
        }
         
        ViaHeaderImpl viaImpl = new ViaHeaderImpl();
        viaImpl.setPort(port);
        viaImpl.setHost(host);
        
        return viaImpl;
    }
    
    /**
     * Creates a ContentTypeHeader based on given media type and sub-type
     * @param <var>type</var> media type
     * @param <var>subType</var> media sub-type
     * @throws IllegalArgumentException if type or subtype are null
     * @throws SipParseException if contentType or contentSubType is not 
     * accepted by implementation
     */
    public ContentTypeHeader createContentTypeHeader(String contentType, 
    	String contentSubType) throws IllegalArgumentException, SipParseException
    {
        if (contentType == null)
        {
			throw new IllegalArgumentException("Null contentType");
        }
         
        if (contentSubType == null)
        {
        	throw new IllegalArgumentException("Null contentSubType"); 
        } 
        
        ContentTypeHeaderImpl contentTypeImpl = new ContentTypeHeaderImpl();
		contentTypeImpl.setContentType(contentType);
		contentTypeImpl.setContentSubType(contentSubType);
        return contentTypeImpl;
    }
    /**
     * Creates a ViaHeader based on given host, port and transport
     * @param <var>host</var> host
     * @param <var>port</var> port
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if host or transport are null
     * @throws SipParseException if host, port or transport are not accepted by
     * implementation
     */
    public ViaHeader createViaHeader(InetAddress host, int port, String transport)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {
        	throw new IllegalArgumentException("Null host"); 
        } 
        if (transport == null)
        {
        	throw new IllegalArgumentException("Null transport"); 
        } 
        if (transport.compareToIgnoreCase(ViaHeaderImpl.UDP) != 0
            && transport.compareToIgnoreCase(ViaHeaderImpl.TCP) != 0)
        {
            throw new SipParseException("Bad Transport: ", transport);
        }
        
        ViaHeaderImpl viaImpl = new ViaHeaderImpl();
		viaImpl.setHost(InetAddressCache.getHostAddress(host));
		viaImpl.setPort(port);
		viaImpl.setTransport(transport);
        return viaImpl;
    }
    
    /**
     * Creates a CSeqHeader based on given sequence number and method
     * @param <var>sequenceNumber</var> sequence number
     * @param <var>method</var> method
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if sequenceNumber or method are not accepted 
     * by implementation
     */
    public CSeqHeader createCSeqHeader(long sequenceNumber, String method)
        throws IllegalArgumentException, SipParseException
    {
        if (method == null)
        {
       		throw new IllegalArgumentException("Null method"); 
       	} 
        
        CSeqHeaderImpl cseqImpl = new CSeqHeaderImpl();
        cseqImpl.setSequenceNumber(sequenceNumber);
        cseqImpl.setMethod(method);
        return cseqImpl;
    }
    
    /**
     * Creates an AcceptEncodingHeader with given content-encoding
     * @param <var>contentEncoding</var> the content-cenoding
     * @throws IllegalArgumentException if contentEncoding is null
     * @throws SipParseException if contentEncoding is not accepted by 
     * implementation
     */
    public AcceptEncodingHeader createAcceptEncodingHeader(String contentEncoding)
        throws IllegalArgumentException, SipParseException
    {
        if (contentEncoding == null)
        {
        	throw new IllegalArgumentException("Null contentEncoding"); 
        } 
        
        AcceptEncodingHeaderImpl acceptImpl = new AcceptEncodingHeaderImpl();
        acceptImpl.setEncoding(contentEncoding);
        return acceptImpl;
    }
    
    /**
     * Creates a DateHeader based on given date string
     * @param <var>date</var> date string
     * @throws IllegalArgumentException if date string is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public DateHeader createDateHeader(String date)
        throws IllegalArgumentException, SipParseException
    {
        if (date == null)
        {
        	throw new IllegalArgumentException("Null date"); 
        } 
        
        DateHeaderImpl dateHeaderImpl = new DateHeaderImpl();
        dateHeaderImpl.setDate(date);
        return dateHeaderImpl;
    }
    
    /**
     * Creates a RecordRouteHeader based on given NameAddress
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public RecordRouteHeader createRecordRouteHeader(NameAddress nameAddress)
        throws IllegalArgumentException, SipParseException
    {
        if (nameAddress == null)
        {
        	throw new IllegalArgumentException("Null nameAddress"); 
        } 
        
        RecordRouteHeaderImpl recordImpl = new RecordRouteHeaderImpl();
        recordImpl.setNameAddress(nameAddress);
        return recordImpl;
    }
    /**
     * Creates an DateHeader based on given Date
     * @param <var>date</var> Date
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public DateHeader createDateHeader(Date date)
        throws IllegalArgumentException, SipParseException
    {
        if (date == null)
        {
        	throw new IllegalArgumentException("Null date"); 
        } 
        
        DateHeaderImpl dateImpl = new DateHeaderImpl();
		dateImpl.setDate(date);
        return dateImpl;
    }
    /**
     * Creates a ResponseKeyHeader based on given scheme
     * @param <var>scheme</var> scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public ResponseKeyHeader createResponseKeyHeader(String scheme)
        throws IllegalArgumentException, SipParseException
    {
        if (scheme == null)
        {
        	throw new IllegalArgumentException("Null scheme"); 
        } 
        
        ResponseKeyHeaderImpl responseHeaderImpl = new ResponseKeyHeaderImpl();
        responseHeaderImpl.setScheme(scheme);
        return responseHeaderImpl;
    }
    
    /**
     * Creates an EncryptionHeader based on given scheme
     * @param <var>scheme</var> scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public EncryptionHeader createEncryptionHeader(String scheme)
        throws IllegalArgumentException, SipParseException
    {
        if (scheme == null)
        {
            throw new IllegalArgumentException("Null scheme");
        }
        
        EncryptionHeaderImpl encryptionImpl = new EncryptionHeaderImpl();
		encryptionImpl.setScheme(scheme);
        return encryptionImpl;
    }
    
    /**
     * Creates a RetryAfterHeader based on given date
     * @param <var>date</var> date
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public RetryAfterHeader createRetryAfterHeader(Date date)
        throws IllegalArgumentException, SipParseException
    {
        if (date == null)
        {
        	throw new IllegalArgumentException("Null date"); 
        } 
        
        RetryAfterHeaderImpl retryAfterImpl = new RetryAfterHeaderImpl();
		retryAfterImpl.setDate(date);
        return retryAfterImpl;
    }
    
    /**
     * Creates an ExpiresHeader based on given number of delta-seconds
     * @param <var>deltaSeconds</var> delta-seconds
     * @throws SipParseException if deltaSeconds is not accepted by 
     * implementation
     */
    public ExpiresHeader createExpiresHeader(long deltaSeconds)
        throws SipParseException
    {
        ExpiresHeaderImpl expiresImpl = new ExpiresHeaderImpl();
		expiresImpl.setDeltaSeconds(deltaSeconds);
        return expiresImpl;
    }
    
    /**
     * Creates a RouteHeader based on given NameAddresss
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public RouteHeader createRouteHeader(NameAddress nameAddress)
        throws IllegalArgumentException, SipParseException
    {
        if (nameAddress == null)
        {
        	throw new IllegalArgumentException("Null nameAddress");
        } 
        RouteHeaderImpl routeImpl = new RouteHeaderImpl();
        routeImpl.setNameAddress(nameAddress);
        return routeImpl;
    }
    
    /**
     * Creates an ExpiresHeader based on given date
     * @param <var>date</var> date
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public ExpiresHeader createExpiresHeader(Date date)
        throws IllegalArgumentException, SipParseException
    {
        if (date == null)
        {
        	throw new IllegalArgumentException("Null date"); 
        } 
        
        ExpiresHeaderImpl expiresImpl = new ExpiresHeaderImpl();
		expiresImpl .setDate(date);
        return expiresImpl ;
    }
    /**
     * Creates a SubjectHeader based on given subject
     * @param <var>subject</var> subject
     * @throws IllegalArgumentException if subject is null
     * @throws SipParseException if subject is not accepted by implementation
     */
    public SubjectHeader createSubjectHeader(String subject)
        throws IllegalArgumentException, SipParseException
    {
        if (subject == null)
        {
        	throw new IllegalArgumentException("Null subject"); 
        } 
        
        SubjectHeaderImpl subjectImpl = new SubjectHeaderImpl();
        subjectImpl.setSubject(subject);
        return subjectImpl ;
    }
    /**
     * Creates an ExpiresHeader based on given date string
     * @param <var>date</var> date string
     * @throws IllegalArgumentException if date is null
     * @throws SipParseException if date is not accepted by implementation
     */
    public ExpiresHeader createExpiresHeader(String date)
        throws IllegalArgumentException, SipParseException
    {
        if (date == null)
            throw new IllegalArgumentException("JAIN-SIP EXCEPTION : date is null ");
        ExpiresHeaderImpl expiresHeaderImpl = new ExpiresHeaderImpl();
        expiresHeaderImpl.setDate(date);
        return expiresHeaderImpl;
    }
    /**
     * Creates a ToHeader based on given NameAddress
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public ToHeader createToHeader(NameAddress nameAddress)
        throws IllegalArgumentException, SipParseException
    {
        if (nameAddress == null)
            throw new IllegalArgumentException("nameAddress is null ");
        ToHeaderImpl toImpl = new ToHeaderImpl();
        toImpl.setNameAddress(nameAddress);
        return toImpl;
    }
    /**
     * Creates a FromHeader based on given NameAddress
     * @param <var>nameAddress</var> NameAddress
     * @throws IllegalArgumentException if nameAddress is null or not from same
     * JAIN SIP implementation
     */
    public FromHeader createFromHeader(NameAddress nameAddress)
        throws IllegalArgumentException, SipParseException
    {
        if (nameAddress == null)
            throw new IllegalArgumentException("nameAddress is null ");
        FromHeaderImpl fromImpl = new FromHeaderImpl();
        fromImpl.setNameAddress(nameAddress);
        return fromImpl;
    }
    /**
     * Creates a UserAgentHeader based on given List of products
     * (Note that the Objects in the List must be Strings)
     * @param <var>products</var> products
     * @throws IllegalArgumentException if products is null, empty, or contains
     * any null elements, or contains any non-String objects
     * @throws SipParseException if any element of products is not accepted by 
     * implementation
     */
    public UserAgentHeader createUserAgentHeader(List products)
        throws IllegalArgumentException, SipParseException
    {
        UserAgentHeaderImpl userAgentImpl = new UserAgentHeaderImpl();
        userAgentImpl.setProducts(products);
        return userAgentImpl;
    }
    
    /**
     * Creates a Header based on given token and value
     * @param <var>name</var> name
     * @param <var>value</var> value
     * @throws IllegalArgumentException if name or value are null
     * @throws SipParseException if name or value are not accepted by 
     * implementation
     */
    public Header createHeader(String name, String value)
        throws IllegalArgumentException, SipParseException
    {
        if (name == null)
        {
        	throw new IllegalArgumentException("Null name"); 
        } 
        if (value == null)
        {
        	throw new IllegalArgumentException("Null value"); 
        } 
        
        
        // Try to parse the given header.
        HeaderImpl hdr = HeaderCreator.createHeader(name);
		hdr.setValue(value);
		try{
			hdr.parse();
		}
		catch (SipParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createHeader", "Failed to parse value = " + value + " for header = " + name);
			}
			
			throw e;
		}
		return hdr;
    }
    
    /**
     * Creates a ViaHeader based on given host
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public ViaHeader createViaHeader(InetAddress host)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {
        	throw new IllegalArgumentException("Null host");
        } 
       
       	ViaHeaderImpl viaImpl = new ViaHeaderImpl();
       	viaImpl.setHost(InetAddressCache.getHostAddress(host));
        viaImpl.setTransport(ViaHeaderImpl.UDP); 
        
        return viaImpl;
    }
    
    /**
     * Creates a HideHeader based on hide value.
     *
     * @param <var>hide</var> hide value
     * @throws IllegalArgumentException if hide is null
     * @throws SipParseException if hide is not accepted by implementation
     */
    public HideHeader createHideHeader(String hide)
        throws IllegalArgumentException, SipParseException
    {
        if (hide == null)
        {
        	throw new IllegalArgumentException("Null hide"); 
        } 
        
        HideHeaderImpl hideImpl = new HideHeaderImpl();
		hideImpl.setHide(hide);
        return hideImpl;
    }
    
    /**
     * Creates a ViaHeader based on given host and port.
     *
     * @param <var>host</var> host
     * @param <var>port</var> port
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host or port is not accepted by 
     * implementation
     */
    public ViaHeader createViaHeader(int port, InetAddress host)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {    
        	throw new IllegalArgumentException("Null host"); 
        } 
        
        ViaHeaderImpl viaImpl = new ViaHeaderImpl();
		viaImpl.setHost(host);
		viaImpl.setPort(port);
		viaImpl.setTransport(ViaHeaderImpl.TCP);
        return viaImpl;
    }
    
    /**
     * Creates a MaxForwardsHeader based on given number of max-forwards
     * @param <var>maxForwards</var> number of max-forwards
     * @throws SipParseException if maxForwards is not accepted by 
     * implementation
     */
    public MaxForwardsHeader createMaxForwardsHeader(int maxForwards)
        throws SipParseException
    {
        MaxForwardsHeaderImpl maxImpl = new MaxForwardsHeaderImpl();
        maxImpl.setMaxForwards(maxForwards);
        return maxImpl;
    }
    /**
     * Creates a ViaHeader based on given host and transport
     * @param <var>host</var> host
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if host or transport are null
     * @throws SipParseException if host or transport are not accepted by 
     * implementation
     */
    public ViaHeader createViaHeader(InetAddress host, String transport)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {
        	throw new IllegalArgumentException("Null host"); 
        } 
        if (transport == null)
        {
        	throw new IllegalArgumentException("Null transport"); 
        } 
        
        ViaHeaderImpl viaImpl = new ViaHeaderImpl();
		viaImpl.setHost(host);
		viaImpl.setTransport(transport);
        return viaImpl;
    }
    
    /**
     * Creates an OrganizationHeader based on given organization
     * @param <var>organization</var> organization
     * @throws IllegalArgumentException if organization is null
     * @throws SipParseException if organization is not accepted by 
     * implementation
     */
    public OrganizationHeader createOrganizationHeader(String organization)
        throws IllegalArgumentException, SipParseException
    {
        if (organization == null)
        {
        	throw new IllegalArgumentException("Null organization"); 
        } 
        
        OrganizationHeaderImpl orgImpl = new OrganizationHeaderImpl();
		orgImpl.setOrganization(organization);
        return orgImpl;
    }
    
    /**
     * Creates a ViaHeader based on given host, port and transport.
     *
     * @param <var>host</var> host
     * @param <var>port</var> port
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if host or transport are null
     * @throws SipParseException if host, port or transport are not accepted by
     * implementation
     */
    public ViaHeader createViaHeader(String host, int port, String transport)
        throws IllegalArgumentException, SipParseException
    {
        if (host == null)
        {
        	throw new IllegalArgumentException("Null host"); 
        } 
        if (transport == null)
        {
        	throw new IllegalArgumentException("Null transport"); 
        } 
        if (port < 0)
        {
        	throw new IllegalArgumentException("Bad port"); 
        } 
        
        ViaHeaderImpl viaImpl = new ViaHeaderImpl();
		viaImpl.setHost(host);
		viaImpl.setPort(port);
		viaImpl.setTransport(transport);
        return viaImpl;
    }
    
    /**
     * Creates a PriorityHeader based on given priority
     * @param <var>priority</var> priority
     * @throws IllegalArgumentException if priority is null
     * @throws SipParseException if priority is not accepted by implementation
     */
    public PriorityHeader createPriorityHeader(String priority)
        throws IllegalArgumentException, SipParseException
    {
        if (priority == null)
        {
        	throw new IllegalArgumentException("Null priority"); 
        } 
        PriorityHeaderImpl priorityImpl = new PriorityHeaderImpl();
        priorityImpl.setPriority(priority);
        return priorityImpl;
    }
    
    /**
     * Creates a WWWAuthenticateHeader based on given scheme
     * @param <var>scheme</var> authentication scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public WWWAuthenticateHeader createWWWAuthenticateHeader(String scheme)
        throws IllegalArgumentException, SipParseException
    {
        if (scheme == null)
        {
        	throw new IllegalArgumentException("Null scheme");
        } 
        
        WWWAuthenticateHeaderImpl wwwImpl = new WWWAuthenticateHeaderImpl();
        wwwImpl.setScheme(scheme);
        return wwwImpl;
    }
    /**
     * Creates a ProxyAuthenticateHeader based on given scheme
     * @param <var>scheme</var> authentication scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public ProxyAuthenticateHeader createProxyAuthenticateHeader(String scheme)
        throws IllegalArgumentException, SipParseException
    {
        if (scheme == null)
        {
        	throw new IllegalArgumentException("Null scheme");
        } 
        
        ProxyAuthenticateHeaderImpl proxyImpl = 
        	new ProxyAuthenticateHeaderImpl();
        proxyImpl.setScheme(scheme);
        return proxyImpl;
    }
    
    /**
     * Creates a ProxyAuthorizationHeader based on given scheme
     * @param <var>scheme</var> authentication scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public ProxyAuthorizationHeader createProxyAuthorizationHeader(String scheme)
        throws IllegalArgumentException, SipParseException
    {
        if (scheme == null)
        {
        	throw new IllegalArgumentException("Null scheme"); 
        } 
        
        ProxyAuthorizationHeaderImpl proxyImpl =
            new ProxyAuthorizationHeaderImpl();
        proxyImpl.setScheme(scheme);
        return proxyImpl;
    }
}
