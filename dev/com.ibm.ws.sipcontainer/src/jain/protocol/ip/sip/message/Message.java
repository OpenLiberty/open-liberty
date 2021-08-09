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

import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.CSeqHeader;
import jain.protocol.ip.sip.header.CallIdHeader;
import jain.protocol.ip.sip.header.ContentLengthHeader;
import jain.protocol.ip.sip.header.ContentTypeHeader;
import jain.protocol.ip.sip.header.DateHeader;
import jain.protocol.ip.sip.header.EncryptionHeader;
import jain.protocol.ip.sip.header.ExpiresHeader;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.Header;
import jain.protocol.ip.sip.header.HeaderIterator;
import jain.protocol.ip.sip.header.HeaderParseException;
import jain.protocol.ip.sip.header.OrganizationHeader;
import jain.protocol.ip.sip.header.RetryAfterHeader;
import jain.protocol.ip.sip.header.TimeStampHeader;
import jain.protocol.ip.sip.header.ToHeader;
import jain.protocol.ip.sip.header.UserAgentHeader;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * This interface represents a generic SIP Message.
 * A Message is either a Request from a client to a server,
 * or a Response from a server to a client.
 * A Message may contain a body(entity) which contains a session description
 * in a format such as SDP (Session Description Protocol). It also contains
 * various Headers which add to the meaning of the Message and the Message body.
 * </p>
 *
 * @see Request
 * @see Response
 * @see Header
 *
 * @version 1.0
 *
 */
public interface Message extends Cloneable, Serializable
{
    
    /**
     * Adds Header to top/bottom of header list (of same type).
     * @param <var>header</var> Header to be added
     * @param <var>top</var> indicates if Header is to be added at top/bottom
     * @throws IllegalArgumentException if header is null or is not from
     * the same JAIN SIP implementation as this Message
     */
    public void addHeader(Header header, boolean top)
                 throws IllegalArgumentException;
    
    /**
     * Sets all Headers of specified name in header list.
     * Note that this method is equivalent to invoking removeHeaders(headerName)
     * followed by addHeaders(headers, top)
     * @param <var>headerName</var> name of Headers to set
     * @param <var>headers</var> List of Headers to be set
     * @throws IllegalArgumentException if headerName or headers is null, if headers is empty,
     * contains any null elements, or contains any objects that are not Header objects from
     * the same JAIN SIP implementation as this Message, or contains any Headers that
     * don't match the specified header name
     */
    public void setHeaders(String headerName, List headers)
                 throws IllegalArgumentException;
    
    /**
     * Sets the first/last Header of header's name in Message's Header list.
     * Note that this method is equivalent to invoking removeHeader(headerName, first)
     * followed by addHeader(header, first)
     * @param <var>header</var> Header to set
     * @param <var>first</var> indicates if first/last Header is to be set
     * @throws IllegalArgumentException if header is null or is not from
     * the same JAIN SIP implementation as this Message
     */
    public void setHeader(Header header, boolean first)
                 throws IllegalArgumentException;
    
    /**
     * Gets boolean value to indicate if Message
     * has OrganizationHeader
     * @return boolean value to indicate if Message
     * has OrganizationHeader
     */
    public boolean hasOrganizationHeader();
    
    /**
     * Gets boolean value to indicate if Message
     * has RetryAfterHeader
     * @return boolean value to indicate if Message
     * has RetryAfterHeader
     */
    public boolean hasRetryAfterHeader();
    
    /**
     * Gets string representation of Message
     * @return string representation of Message
     */
    public String toString();
    
    /**
     * Gets boolean value to indicate if Message
     * has AcceptEncodingHeaders
     * @return boolean value to indicate if Message
     * has AcceptEncodingHeaders
     */
    public boolean hasAcceptEncodingHeaders();
    
    /**
     * Gets boolean value to indicate if Message
     * has ExpiresHeader
     * @return boolean value to indicate if Message
     * has ExpiresHeader
     */
    public boolean hasExpiresHeader();
    
    /**
     * Removes first (or last) Header of specified name from Message's Header list.
     * Note if no Headers of specified name exist the method has no effect
     * @param <var>headerName</var> name of Header to be removed
     * @param <var>first</var> indicates whether first or last Header of specified name is to be removed
     * @throws IllegalArgumentException if headerName is null
     */
    public void removeHeader(String headerName, boolean first)
                 throws IllegalArgumentException;
    
    /**
     * Removes body from Message and all associated entity headers (if body exists)
     */
    public void removeBody();
    
    /**
     * Removes all Headers of specified name from Message's Header list.
     * Note if no Headers of specified name exist the method has no effect
     * @param <var>headername</var> name of Headers to be removed
     * @throws IllegalArgumentException if headerType is null
     */
    public void removeHeaders(String headerName)
                 throws IllegalArgumentException;
    
    /**
     * Gets boolean value to indicate if Message
     * has AcceptHeaders
     * @return boolean value to indicate if Message
     * has AcceptHeaders
     */
    public boolean hasAcceptHeaders();
    
    /**
     * Gets HeaderIterator of all Headers in Message.
     * Note that order of Headers in HeaderIterator is same as order in
     * Message
     * (Returns null if no Headers exist)
     * @return HeaderIterator of all Headers in Message
     */
    public HeaderIterator getHeaders();
    
    /**
     * Gets boolean value to indicate if Message
     * has AcceptLanguageHeaders
     * @return boolean value to indicate if Message
     * has AcceptLanguageHeaders
     */
    public boolean hasAcceptLanguageHeaders();
    
    /**
     * Gets HeaderIterator of all Headers of specified name in Message.
     * Note that order of Headers in HeaderIterator is same as order they appear in
     * Message
     * (Returns null if no Headers of specified name exist)
     * @param <var>headerName</var> name of Headers to return
     * @return HeaderIterator of all Headers of specified name in Message
     * @throws IllegalArgumentException if headerName is null
     */
    public HeaderIterator getHeaders(String headerName)
                           throws IllegalArgumentException;
    
    /**
     * Gets boolean value to indicate if Message
     * has ContactHeaders
     * @return boolean value to indicate if Message
     * has ContactHeaders
     */
    public boolean hasContactHeaders();
    
    /**
     * Gets first (or last) Header of specified name in Message
     * (Returns null if no Headers of specified name exist)
     * @param <var>headerName</var> name of Header to return
     * @param <var>first</var> indicates whether the first or last Header of specified name is required
     * @return first (or last) Header of specified name in Message's Header list
     * @throws IllegalArgumentException if headername is null
     * @throws HeaderParseException if implementation could not parse header value
     */
    public Header getHeader(String headerName, boolean first)
                   throws IllegalArgumentException,HeaderParseException;
    
    /**
     * Gets boolean value to indicate if Message
     * has RecordRouteHeaders
     * @return boolean value to indicate if Message
     * has RecordRouteHeaders
     */
    public boolean hasRecordRouteHeaders();
    
    /**
     * Gets boolean value to indicate if Message
     * has any headers
     * @return boolean value to indicate if Message
     * has any headers
     */
    public boolean hasHeaders();
    
    /**
     * Gets body of Message as byte array
     * (Returns null if no body exists)
     * @return body of Message as byte array
     */
    public byte[] getBodyAsBytes();
    
    /**
     * Gets boolean value to indicate if Message
     * has any headers of specified name
     * @return boolean value to indicate if Message
     * has any headers of specified name
     * @param <var>headerName</var> header name
     * @throws IllegalArgumentException if headerName is null
     */
    public boolean hasHeaders(String headerName)
                    throws IllegalArgumentException;
    
    /**
     * Returns boolean value to indicate if Message is a Request.
     * @return boolean value to indicate if Message is a Request
     */
    public boolean isRequest();
    
    /**
     * Gets CallIdHeader of Message.
     * (Returns null if no CallIdHeader exists)
     * @return CallIdHeader of Message
     */
    public CallIdHeader getCallIdHeader();
    
    /**
     * Sets ContentLengthHeader of Message.
     * @param <var>contentLengthHeader</var> ContentLengthHeader to set
     * @throws IllegalArgumentException if contentLengthHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setContentLengthHeader(ContentLengthHeader contentLengthHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets CallIdHeader of Message.
     * @param <var>callIdHeader</var> CallIdHeader to set
     * @throws IllegalArgumentException if callIdHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setCallIdHeader(CallIdHeader callIdHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets AcceptHeaders of Message.
     * @param <var>acceptHeaders</var> List of AcceptHeaders to set
     * @throws IllegalArgumentException if acceptHeaders is null, empty, contains
     * any elements that are null or not AcceptHeaders from the same
     * JAIN SIP implementation
     */
    public void setAcceptHeaders(List acceptHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Gets CSeqHeader of Message.
     * (Returns null if no CSeqHeader exists)
     * @return CSeqHeader of Message
     */
    public CSeqHeader getCSeqHeader();
    
    /**
     * Sets AcceptEncodingHeaders of Message.
     * @param <var>acceptEncodingHeaders</var> List of AcceptEncodingHeaders to set
     * @throws IllegalArgumentException if acceptEncodingHeaders is null, empty, contains
     * any elements that are null or not AcceptEncodingHeaders from the same
     * JAIN SIP implementation
     */
    public void setAcceptEncodingHeaders(List acceptEncodingHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Sets CSeqHeader of Message.
     * @param <var>cSeqHeader</var> CSeqHeader to set
     * @throws IllegalArgumentException if cSeqHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setCSeqHeader(CSeqHeader cSeqHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets AcceptLanguageHeaders of Message.
     * @param <var>acceptLanguageHeaders</var> List of AcceptLanguageHeaders to set
     * @throws IllegalArgumentException if acceptLanguageHeaders is null, empty, contains
     * any elements that are null or not AcceptLanguageHeaders from the same
     * JAIN SIP implementation
     */
    public void setAcceptLanguageHeaders(List acceptLanguageHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Gets ToHeader of Message.
     * (Returns null if no CSeqHeader exists)
     * @return ToHeader of Message
     * @throws HeaderNotSetException if no ToHeader exists
     */
    public ToHeader getToHeader();
    
    /**
     * Sets ExpiresHeader of Message.
     * @param <var>expiresHeader</var> ExpiresHeader to set
     * @throws IllegalArgumentException if expiresHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setExpiresHeader(ExpiresHeader expiresHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets ToHeader of Message.
     * @param <var>toHeader</var> ToHeader to set
     * @throws IllegalArgumentException if toHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setToHeader(ToHeader toHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets ContactHeaders of Message.
     * @param <var>contactHeaders</var> List of ContactHeaders to set
     * @throws IllegalArgumentException if contactHeaders is null, empty, contains
     * any elements that are null or not ContactHeaders from the same
     * JAIN SIP implementation
     */
    public void setContactHeaders(List contactHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Gets FromHeader of Message.
     * (Returns null if no CSeqHeader exists)
     * @return FromHeader of Message
     * @throws HeaderNotSetException if no FromHeader exists
     */
    public FromHeader getFromHeader();
    
    /**
     * Sets OrganizationHeader of Message.
     * @param <var>organizationHeader</var> OrganizationHeader to set
     * @throws IllegalArgumentException if organizationHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setOrganizationHeader(OrganizationHeader organizationHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets FromHeader of Message.
     * @param <var>fromHeader</var> FromHeader to set
     * @throws IllegalArgumentException if fromHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setFromHeader(FromHeader fromHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets RecordRouteHeaders of Message.
     * @param <var>recordRouteHeaders</var> List of RecordRouteHeaders to set
     * @throws IllegalArgumentException if recordRouteHeaders is null, empty, contains
     * any elements that are null or not RecordRouteHeaders from the same
     * JAIN SIP implementation
     */
    public void setRecordRouteHeaders(List recordRouteHeaders)
                 throws IllegalArgumentException;
    
    /////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Gets HeaderIterator of ViaHeaders of Message.
     * (Returns null if no ViaHeaders exist)
     * @return HeaderIterator of ViaHeaders of Message
     */
    public HeaderIterator getViaHeaders();
    
    /**
     * Sets RetryAfterHeader of Message.
     * @param <var>retryAfterHeader</var> RetryAfterHeader to set
     * @throws IllegalArgumentException if retryAfterHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setRetryAfterHeader(RetryAfterHeader retryAfterHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets ViaHeaders of Message.
     * @param <var>viaHeaders</var> List of ViaHeaders to set
     * @throws IllegalArgumentException if viaHeaders is null, empty, contains
     * any elements that are null or not ViaHeaders from the same
     * JAIN SIP implementation
     */
    public void setViaHeaders(List viaHeaders)
                 throws IllegalArgumentException;
    
    /**
     * Sets body of Message (with ContentTypeHeader)
     * @param <var>body</var> body to set
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if body or contentTypeHeader is null, or
     * contentTypeHeader is not from same JAIN SIP implementation
     * @throws SipParseException if body is not accepted by implementation
     */
    public void setBody(String body, ContentTypeHeader contentTypeHeader)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets boolean value to indicate if Message
     * has ViaHeaders
     * @return boolean value to indicate if Message
     * has ViaHeaders
     */
    public boolean hasViaHeaders();
    
    /**
     * Gets version minor of Message.
     * @return version minor of Message
     * @throws SipParseException if implementation could not parse version minor
     */
    public int getVersionMinor()
                throws SipParseException;
    
    /**
     * Removes ViaHeaders from Message (if any exist)
     */
    public void removeViaHeaders();
    
    /**
     * Indicates whether some other Object is "equal to" this Message
     * (Note that obj must have the same Class as this Message - this means that it
     * must be from the same JAIN SIP implementation)
     * @param <var>obj</var> the Object with which to compare this Message
     * @returns true if this Message is "equal to" the obj
     * argument; false otherwise
     */
    public boolean equals(Object object);
    
    /**
     * Gets ContentTypeHeader of Message.
     * (Returns null if no ContentTypeHeader exists)
     * @return ContentTypeHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public ContentTypeHeader getContentTypeHeader()
                              throws HeaderParseException;
    
    /**
     * Removes ContentLengthHeader from Message (if it exists)
     */
    public void removeContentLengthHeader();
    
    /**
     * Sets ContentTypeHeader of Message.
     * @param <var>contentTypeHeader</var> ContentTypeHeader to set
     * @throws IllegalArgumentException if contentTypeHeader is null
     * or not from same JAIN SIP implementation
     * @throws SipException if Message does not contain body
     */
    public void setContentTypeHeader(ContentTypeHeader contentTypeHeader)
                 throws IllegalArgumentException,SipException;
    
    /**
     * Gets HeaderIterator of AcceptHeaders of Message.
     * (Returns null if no AcceptHeaders exist)
     * @return HeaderIterator of AcceptHeaders of Message
     */
    public HeaderIterator getAcceptHeaders();
    
    /**
     * Gets boolean value to indicate if Message
     * has ContentTypeHeader
     * @return boolean value to indicate if Message
     * has ContentTypeHeader
     */
    public boolean hasContentTypeHeader();
    
    /**
     * Removes AcceptHeaders from Message (if any exist)
     */
    public void removeAcceptHeaders();
    
    /**
     * Removes ContentTypeHeader from Message (if it exists)
     */
    public void removeContentTypeHeader();
    
    /**
     * Gets HeaderIterator of AcceptEncodingHeaders of Message.
     * (Returns null if no AcceptEncodingHeaders exist)
     * @return HeaderIterator of AcceptEncodingHeaders of Message
     */
    public HeaderIterator getAcceptEncodingHeaders();
    
    /**
     * Gets DateHeader of Message.
     * (Returns null if no DateHeader exists)
     * @return DateHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public DateHeader getDateHeader()
                       throws HeaderParseException;
    
    /**
     * Removes AcceptEncodingHeaders from Message (if any exist)
     */
    public void removeAcceptEncodingHeaders();
    
    /**
     * Gets boolean value to indicate if Message
     * has DateHeader
     * @return boolean value to indicate if Message
     * has DateHeader
     */
    public boolean hasDateHeader();
    
    /**
     * Gets HeaderIterator of AcceptLanguageHeaders of Message.
     * (Returns null if no AcceptLanguageHeaders exist)
     * @return HeaderIterator of AcceptLanguageHeaders of Message
     */
    public HeaderIterator getAcceptLanguageHeaders();
    
    /**
     * Sets DateHeader of Message.
     * @param <var>dateHeader</var> DateHeader to set
     * @throws IllegalArgumentException if dateHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setDateHeader(DateHeader dateHeader)
                 throws IllegalArgumentException;
    
    /**
     * Removes AcceptLanguageHeaders from Message (if any exist)
     */
    public void removeAcceptLanguageHeaders();
    
    /**
     * Removes DateHeader from Message (if it exists)
     */
    public void removeDateHeader();
    
    /**
     * Gets ExpiresHeader of Message.
     * (Returns null if no ExpiresHeader exists)
     * @return ExpiresHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public ExpiresHeader getExpiresHeader()
                          throws HeaderParseException;
    
    /**
     * Gets EncryptionHeader of Message.
     * (Returns null if no EncryptionHeader exists)
     * @return EncryptionHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public EncryptionHeader getEncryptionHeader()
                             throws HeaderParseException;
    
    /**
     * Removes ExpiresHeader from Message (if it exists)
     */
    public void removeExpiresHeader();
    
    /**
     * Gets boolean value to indicate if Message
     * has EncryptionHeader
     * @return boolean value to indicate if Message
     * has EncryptionHeader
     */
    public boolean hasEncryptionHeader();
    
    /**
     * Gets HeaderIterator of ContactHeaders of Message.
     * (Returns null if no ContactHeaders exist)
     * @return HeaderIterator of ContactHeaders of Message
     */
    public HeaderIterator getContactHeaders();
    
    /**
     * Sets EncryptionHeader of Message.
     * @param <var>encryptionHeader</var> EncryptionHeader to set
     * @throws IllegalArgumentException if encryptionHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setEncryptionHeader(EncryptionHeader encryptionHeader)
                 throws IllegalArgumentException;
    
    /**
     * Removes ContactHeaders from Message (if any exist)
     */
    public void removeContactHeaders();
    
    /**
     * Removes EncryptionHeader from Message (if it exists)
     */
    public void removeEncryptionHeader();
    
    /**
     * Gets OrganizationHeader of Message.
     * (Returns null if no OrganizationHeader exists)
     * @return OrganizationHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public OrganizationHeader getOrganizationHeader()
                               throws HeaderParseException;
    
    /**
     * Gets UserAgentHeader of Message.
     * (Returns null if no UserAgentHeader exists)
     * @return UserAgentHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public UserAgentHeader getUserAgentHeader()
                            throws HeaderParseException;
    
    /**
     * Removes OrganizationHeader from Message (if it exists)
     */
    public void removeOrganizationHeader();
    
    /**
     * Gets boolean value to indicate if Message
     * has UserAgentHeader
     * @return boolean value to indicate if Message
     * has UserAgentHeader
     */
    public boolean hasUserAgentHeader();
    
    /**
     * Gets HeaderIterator of RecordRouteHeaders of Message.
     * (Returns null if no RecordRouteHeaders exist)
     * @return HeaderIterator of RecordRouteHeaders of Message
     */
    public HeaderIterator getRecordRouteHeaders();
    
    /**
     * Sets UserAgentHeader of Message.
     * @param <var>userAgentHeader</var> UserAgentHeader to set
     * @throws IllegalArgumentException if userAgentHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setUserAgentHeader(UserAgentHeader userAgentHeader)
                 throws IllegalArgumentException;
    
    /**
     * Removes RecordRouteHeaders from Message (if any exist)
     */
    public void removeRecordRouteHeaders();
    
    /**
     * Removes UserAgentHeader from Message (if it exists)
     */
    public void removeUserAgentHeader();
    
    /**
     * Gets RetryAfterHeader of Message.
     * (Returns null if no RetryAfterHeader exists)
     * @return RetryAfterHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public RetryAfterHeader getRetryAfterHeader()
                             throws HeaderParseException;
    
    /**
     * Gets TimeStampHeader of Message.
     * (Returns null if no TimeStampHeader exists)
     * @return TimeStampHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public TimeStampHeader getTimeStampHeader()
                            throws HeaderParseException;
    
    /**
     * Removes RetryAfterHeader from Message (if it exists)
     */
    public void removeRetryAfterHeader();
    
    /**
     * Gets boolean value to indicate if Message
     * has TimeStampHeader
     * @return boolean value to indicate if Message
     * has TimeStampHeader
     */
    public boolean hasTimeStampHeader();
    
    /**
     * Gets body of Message as String
     * (Returns null if no body exists)
     * @return body of Message as String
     */
    public String getBodyAsString();
    
    /**
     * Removes TimeStampHeader from Message (if it exists)
     */
    public void removeTimeStampHeader();
    
    /**
     * Gets boolean value to indicate if Message
     * has body
     * @return boolean value to indicate if Message
     * has body
     */
    public boolean hasBody();
    
    /**
     * Sets TimeStampHeader of Message.
     * @param <var>timeStampHeader</var> TimeStampHeader to set
     * @throws IllegalArgumentException if timeStampHeader is null
     * or not from same JAIN SIP implementation
     */
    public void setTimeStampHeader(TimeStampHeader timeStampHeader)
                 throws IllegalArgumentException;
    
    /**
     * Sets body of Message (with ContentTypeHeader)
     * @param <var>body</var> body to set
     * @param <var>contentTypeHeader</var> ContentTypeHeader
     * @throws IllegalArgumentException if body or contentTypeHeader is null, or
     * contentTypeHeader is not from same JAIN SIP implementation
     * @throws SipParseException if body is not accepted by implementation
     */
    public void setBody(byte[] body, ContentTypeHeader contentTypeHeader)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets HeaderIterator of ContentEncodingHeaders of Message.
     * (Returns null if no ContentEncodingHeaders exist)
     * @return HeaderIterator of ContentEncodingHeaders of Message
     */
    public HeaderIterator getContentEncodingHeaders();
    
    /**
     * Gets version major of Message.
     * @return version major of Message
     * @throws SipParseException if implementation could not parse version major
     */
    public int getVersionMajor()
                throws SipParseException;
    
    /**
     * Gets boolean value to indicate if Message
     * has ContentEncodingHeaders
     * @return boolean value to indicate if Message
     * has ContentEncodingHeaders
     */
    public boolean hasContentEncodingHeaders();
    
    /**
     * Sets version of Message. Note that the version defaults to 2.0.
     * (i.e. version major of 2 and version minor of 0)
     * @param <var>versionMajor</var> version major
     * @param <var>versionMinor</var> version minor
     * @throws SipParseException if versionMajor or versionMinor are not accepted
     * by implementation
     */
    public void setVersion(int versionMajor, int versionMinor)
                 throws SipParseException;
    
    /**
     * Removes ContentEncodingHeaders from Message (if any exist)
     */
    public void removeContentEncodingHeaders();
    
    /**
     * Returns start line of Message
     * @return start line of Message
     */
    public String getStartLine();
    
    /**
     * Sets ContentEncodingHeaders of Message.
     * @param <var>contentEncodingHeaders</var> List of ContentEncodingHeaders to set
     * @throws IllegalArgumentException if contentEncodingHeaders is null, empty, contains
     * any elements that are null or not ContentEncodingHeaders from the same
     * JAIN SIP implementation
     */
    public void setContentEncodingHeaders(List contentEncodingHeaders)
                 throws IllegalArgumentException,SipException;
    
    /**
     * Creates and returns copy of Message
     * @returns copy of Message
     */
    public Object clone();
    
    /**
     * Gets ContentLengthHeader of Message.
     * (Returns null if no ContentLengthHeader exists)
     * @return ContentLengthHeader of Message
     * @throws HeaderParseException if implementation could not parse header value
     */
    public ContentLengthHeader getContentLengthHeader()
                                throws HeaderParseException;
    
    /**
     * Adds list of Headers to top/bottom of header list (of same type).
     * Note that the Headers are added in same order as in List.
     * @param <var>headerName</var> name of Headers to add
     * @param <var>headers</var> List of Headers to be added
     * @param <var>top</var> indicates if Headers are to be added at top/bottom
     * of header list
     * @throws IllegalArgumentException if headerNmae is null, if headers is null, empty, contains any
     * null objects, or contains any objects that are not Header objects from
     * the same JAIN SIP implementation as this Message, or contains any Headers that
     * don't match the specified header name
     */
    public void addHeaders(String headerName, List headers, boolean top)
                 throws IllegalArgumentException;
    
    /**
     * Gets boolean value to indicate if Message
     * has ContentLengthHeader
     * @return boolean value to indicate if Message
     * has ContentLengthHeader
     */
    public boolean hasContentLengthHeader();
}
