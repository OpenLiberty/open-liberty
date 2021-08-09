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
package jain.protocol.ip.sip.address;

import jain.protocol.ip.sip.Parameters;
import jain.protocol.ip.sip.SipException;
import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.FromHeader;
import jain.protocol.ip.sip.header.ToHeader;

import java.net.InetAddress;
import java.util.Iterator;

/**
 * <p>
 * This interface represents a SIP URL.
 * SipURLs are used to indicate the originator
 * (FromHeader), current destination (RequestURI) and final recipient
 * (ToHeader) of a SIP Request, and to specify redirection addresses
 * (ContactHeader). A SipURL can also be embedded in web pages or other
 * hyperlinks to indicate that a particular user or service can be called
 * via SIP. When used as a hyperlink, the SipURL indicates the use of the INVITE method.
 * </p><p>
 * The SipURL scheme is defined to allow setting headers and the message-body.
 * This corresponds to the use of mailto: URLs. It makes it
 * possible, for example, to specify the subject, urgency or
 * media types of calls initiated through a web page or as
 * part of an email message.
 * </p><p>
 * The components of the SipURL have the following meanings:
 * <LI><b>UserName:</b> If the host is an Internet telephony gateway, the UserName
 * may also encode a telephone number. Thus, a URL parameter, UserType, is added
 * to distinguish telephone numbers from user names. The phone identifier is to be used when
 * connecting to a telephony gateway. Even without this parameter,
 * recipients of SipURLs may interpret the UserName part as a phone
 * number if local restrictions on the name space for user name
 * allow it.</LI>
 * <LI><b>UserPassword:</b> The use of passwords in the userinfo is not
 * recommended, because the passing of authentication information
 * in clear text (such as URIs) has proven to be a security risk in
 * almost every case where it has been used.</LI>
 * <LI><b>Host:</b> The mailto: URL and RFC 822 email addresses require that
 * numeric host addresses ("host numbers") are enclosed in square
 * brackets (presumably, since host names might be numeric), while
 * host numbers without brackets are used for all other URLs. The
 * SipURL requires the latter form, without brackets.</LI>
 * <LI><b>Port:</b> The port number to send a Request to.</LI>
 * <LI><b>URL parameters:</b> SipURLs can define specific parameters of the
 * Request. The Transport parameter determines
 * the transport mechanism (UDP or TCP). UDP is to be assumed
 * when no explicit transport parameter is included. The MAddr
 * parameter provides the server address to be contacted for this
 * user, overriding the address supplied in the host field. This
 * address is typically a multicast address, but could also be the
 * address of a backup server. The TTL parameter determines the
 * time-to-live value of the UDP multicast packet and must only be
 * used if maddr is a multicast address and the transport protocol
 * is UDP. The UserType parameter was described above. The Transport,
 * MAddr, and TTL parameters must not be used in the FromHeader,
 * ToHeader and the RequestURI; they are ignored if present.</LI>
 * <LI><b>Headers:</b> Headers of the SIP Request can be defined
 * within a SIP URL. The special header name "body" indicates
 * that the associated header value is the message-body of the SIP
 * INVITE Request. Headers must not be used in the FromHeader,
 * ToHeader and the RequestURI; they are ignored if present.</LI>
 * <LI><b>Method:</b> The method of the SIP Request can be specified with the
 * Method parameter.  This parameter must not be used in the FromHeader,
 * ToHeader and the RequestURI; they are ignored if present.</LI>
 * <p>
 * Within a SIP Message, SipURLs are used to indicate the source and
 * intended destination of a RequestMessage, redirection addresses and the
 * current destination of a RequestMessage. Normally all these Headers will
 * contain SipURLs.
 * </p><p>
 * SipURLs are case-insensitive. All SipURL parameters are included when
 * comparing for equality.
 * </p>
 *
 * @see Request
 * @see FromHeader
 * @see ToHeader
 *
 * @version 1.0
 *
 */
public interface SipURL extends URI, Parameters
{
    
    /**
     * Gets boolean value to indicate if SipURL
     * has ISDN subaddress
     * @return boolean value to indicate if SipURL
     * has ISDN subaddress
     */
    public boolean hasIsdnSubAddress();
    
    /**
     * Removes ISDN subaddress from SipURL (if it exists)
     */
    public void removeIsdnSubAddress();
    
    /**
     * Gets post dial of SipURL
     * (Returns null if post dial does not exist)
     * @return post dial of SipURL
     */
    public String getPostDial();
    
    /**
     * Sets method of SipURL
     * @param <var>method</var> method
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if method is not accepted by implementation
     */
    public void setMethod(String method)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets boolean value to indicate if SipURL
     * has MAddr
     * @return boolean value to indicate if SipURL
     * has MAddr
     */
    public boolean hasMAddr();
    
    /**
     * Sets value of header
     * @param <var>name</var> name of header
     * @param <var>value</var> value of header
     * @throws IllegalArgumentException if name or value is null
     * @throws SipParseException if name or value is not accepted by implementation
     */
    public void setHeader(String name, String value)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets transport of SipURL
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if transport is null
     * @throws SipParseException if transport is not accepted by implementation
     */
    public void setTransport(String transport)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets user type of SipURL
     * @param <var>userType</var> user type
     * @throws IllegalArgumentException if userType is null
     * @throws SipParseException if userType is not accepted by implementation
     */
    public void setUserType(String userType)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets boolean value to indicate if SipURL
     * has post dial
     * @return boolean value to indicate if SipURL
     * has post dial
     */
    public boolean hasPostDial();
    
    /**
     * Returns boolean value to indicate if the SipURL
     * has a global phone user
     * @return boolean value to indicate if the SipURL
     * has a global phone user
     * @throws SipException if user type is not USER_TYPE_PHONE
     */
    public boolean isGlobal()
                    throws SipException;
    
    /**
     * Removes post dial from SipURL (if it exists)
     */
    public void removePostDial();
    
    /**
     * Removes all parameters from Parameters (if any exist)
     */
    public void removeHeaders();
    
    /**
     * Gets user name of SipURL
     * (Returns null if user name does not exist)
     * @return user name of SipURL
     */
    public String getUserName();
    
    /**
     * Gets boolean value to indicate if SipURL
     * has user type
     * @return boolean value to indicate if SipURL
     * has user type
     */
    public boolean hasUserType();
    
    /**
     * Gets boolean value to indicate if SipURL
     * has user name
     * @return boolean value to indicate if SipURL
     * has user name
     */
    public boolean hasUserName();
    
    /**
     * Gets boolean value to indicate if SipURL
     * has method
     * @return boolean value to indicate if SipURL
     * has method
     */
    public boolean hasMethod();
    
    /**
     * Removes user name from SipURL (if it exists)
     */
    public void removeUserName();
    
    /**
     * Sets post dial of SipURL
     * @param <var>postDial</var> post dial
     * @throws IllegalArgumentException if postDial is null
     * @throws SipException if user type is not USER_TYPE_PHONE
     * @throws SipParseException if postDial is not accepted by implementation
     */
    public void setPostDial(String postDial)
                 throws IllegalArgumentException,SipException,SipParseException;
    
    /**
     * Sets user name of SipURL
     * @param <var>userName</var> user name
     * @throws IllegalArgumentException if userName is null
     * @throws SipParseException if userName is not accepted by implementation
     */
    public void setUserName(String userName)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets MAddr of SipURL
     * @param <var>mAddr</var> MAddr
     * @throws IllegalArgumentException if mAddr is null
     * @throws SipParseException if mAddr is not accepted by implementation
     */
    public void setMAddr(String mAddr)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets user password of SipURL
     * (Returns null if user pasword does not exist)
     * @return user password of SipURL
     */
    public String getUserPassword();
    
    /**
     * Gets Iterator of header names
     * (Note - objects returned by Iterator are Strings)
     * (Returns null if no headers exist)
     * @return Iterator of header names
     */
    public Iterator getHeaders();
    
    /**
     * Gets boolean value to indicate if SipURL
     * has user password
     * @return boolean value to indicate if SipURL
     * has user password
     */
    public boolean hasUserPassword();
    
    /**
     * Gets boolean value to indicate if SipUrl
     * has specified header
     * @return boolean value to indicate if SipUrl
     * has specified header
     * @throws IllegalArgumentException if name is null
     */
    public boolean hasHeader(String name)
                    throws IllegalArgumentException;
    
    /**
     * Removes user password from SipURL (if it exists)
     */
    public void removeUserPassword();
    
    /**
     * Gets ISDN subaddress of SipURL
     * (Returns null if ISDN subaddress does not exist)
     * @return ISDN subaddress of SipURL
     */
    public String getIsdnSubAddress();
    
    /**
     * Sets user password of SipURL
     * @param <var>userPassword</var> user password
     * @throws IllegalArgumentException if userPassword is null
     * @throws SipException if user name does not exist
     * @throws SipParseException if userPassword is not accepted by implementation
     */
    public void setUserPassword(String userPassword)
                 throws IllegalArgumentException,SipException,SipParseException;
    
    /**
     * Gets user type of SipURL
     * (Returns null if user type does not exist)
     * @return user type of SipURL
     */
    public String getUserType();
    
    /**
     * Gets host of SipURL
     * @return host of SipURL
     */
    public String getHost();
    
    /**
     * Removes user type from SipURL (if it exists)
     */
    public void removeUserType();
    
    /**
     * Sets host of SipURL
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(String host)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets method of SipURL
     * (Returns null if method does not exist)
     * @return method of SipURL
     */
    public String getMethod();
    
    /**
     * Sets host of SipURL
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(InetAddress host)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Removes method from SipURL (if it exists)
     */
    public void removeMethod();
    
    /**
     * Gets port of SipURL
     * (Returns negative int if port does not exist)
     * @return port of SipURL
     */
    public int getPort();
    
    /**
     * Sets ISDN subaddress of SipURL
     * @param <var>isdnSubAddress</var> ISDN subaddress
     * @throws IllegalArgumentException if isdnSubAddress is null
     * @throws SipException if user type is not USER_TYPE_PHONE
     * @throws SipParseException if isdnSubAddress is not accepted by implementation
     */
    public void setIsdnSubAddress(String isdnSubAddress)
                 throws IllegalArgumentException,SipException,SipParseException;
    
    /**
     * Gets boolean value to indicate if SipURL
     * has port
     * @return boolean value to indicate if SipURL
     * has port
     */
    public boolean hasPort();
    
    /**
     * Gets MAddr of SipURL
     * (Returns null if MAddr does not exist)
     * @return MAddr of SipURL
     */
    public String getMAddr();
    
    /**
     * Removes port from SipURL (if it exists)
     */
    public void removePort();
    
    /**
     * Removes MAddr from SipURL (if it exists)
     */
    public void removeMAddr();
    
    /**
     * Sets port of SipURL
     * @param <var>port</var> port
     * @throws SipParseException if port is not accepted by implementation
     */
    public void setPort(int port)
                 throws SipParseException;
    
    /**
     * Sets MAddr of SipURL
     * @param <var>mAddr</var> MAddr
     * @throws IllegalArgumentException if mAddr is null
     * @throws SipParseException if mAddr is not accepted by implementation
     */
    public void setMAddr(InetAddress mAddr)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets TTL of SipURL
     * (Returns negative int if TTL does not exist)
     * @return TTL of SipURL
     */
    public int getTTL();
    
    /**
     * Sets phone user of SipURL to be global or local
     * @param <var>global</var> boolean value indicating if phone user should be global
     * @throws SipException if user type is not USER_TYPE_PHONE
     */
    public void setGlobal(boolean global)
                 throws SipException,SipParseException;
    
    /**
     * Gets boolean value to indicate if SipURL
     * has TTL
     * @return boolean value to indicate if SipURL
     * has TTL
     */
    public boolean hasTTL();
    
    /**
     * Gets the value of specified header
     * (Returns null if header does not exist)
     * @param <var>name</var> name of header to retrieve
     * @return the value of specified header
     * @throws IllegalArgumentException if header is null
     */
    public String getHeader(String name)
                   throws IllegalArgumentException;
    
    /**
     * Removes TTL from SipURL (if it exists)
     */
    public void removeTTL();
    
    /**
     * Gets boolean value to indicate if SipURL
     * has any headers
     * @return boolean value to indicate if SipURL
     * has any headers
     */
    public boolean hasHeaders();
    
    /**
     * Sets TTL of SipURL
     * @param <var>ttl</var> TTL
     * @throws SipParseException if ttl is not accepted by implementation
     */
    public void setTTL(int ttl)
                 throws SipParseException;
    
    /**
     * Removes specified header from SipURL (if it exists)
     * @param <var>name</var> name of header
     * @throws IllegalArgumentException if name is null
     */
    public void removeHeader(String name)
                 throws IllegalArgumentException;
    
    /**
     * Gets transport of SipURL
     * (Returns null if transport does not exist)
     * @return transport of SipURL
     */
    public String getTransport();
    
    /**
     * Gets boolean value to indicate if SipURL
     * has transport
     * @return boolean value to indicate if SipURL
     * has transport
     */
    public boolean hasTransport();
    
    /**
     * Removes transport from SipURL (if it exists)
     */
    public void removeTransport();
    
    /**
     * Phone User Type constant
     */
    public static final String USER_TYPE_PHONE = "phone";
    
    /**
     * IP User Type constant
     */
    public static final String USER_TYPE_IP = "ip";
}
