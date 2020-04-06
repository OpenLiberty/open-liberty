/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import java.util.Iterator;

/**
 * Represents <code>sip</code> and <code>sips</code> URIs.
 * 
 * <p>SIP and SIPS URIs are used for addressing. They are similar to email
 * addresses in that they are of the form <em>user@host</em> where
 * <em>user</em> is either a user name or telephone number, and <em>host</em>
 * is a host or domain name, or a numeric IP address. Additionally, SIP and
 * SIPS URIs may contain parameters and headers (although headers are not
 * legal in all contexts).
 * 
 * <p>Syntactically, SIP and SIPS URIs are identical except for the name of
 * URI scheme. The semantics differs in that the SIPS scheme implies that
 * the identified resource is to be contacted using TLS. The following quote
 * is from RFC 3261:
 * 
 * <blockquote>
 * "A SIPS URI specifies that the resource be contacted securely. This
   means, in particular, that TLS is to be used between the UAC and the
   domain that owns the URI. From there, secure communications are used
   to reach the user, where the specific security mechanism depends on
   the policy of the domain. Any resource described by a SIP URI can be
   "upgraded" to a SIPS URI by just changing the scheme, if it is
   desired to communicate with that resource securely."
 * </blockquote>
 *
 * <p>Because <code>sip</code> and <code>sips</code> URIs are syntactically
 * identical and because they're used the same way, they're both represented
 * by the <code>SipURI</code> interface.
 * 
 * <p>The string form of SIP and SIPS URIs may contain escaped characters.
 * The SIP
 * servlet container is responsible for unescaping those characters before
 * presenting URIs to servlets. Likewise, string values passed to setters 
 * for various SIP(S) URI components may contain reserved or excluded
 * characters that need escaping before being used. The container is
 * responsible for escaping those values.
 * 
 * @see Address
 * @see SipFactory#createSipURI
 * @see SipServletRequest#getRequestURI
 */
public interface SipURI extends URI {
	
	/**
	 * Compares the given SipURI with this SipURI. The rules 
	 * specified in section 19.1.4 RFC 3261 must be used for comparison.
	 * 
	 * @param o the URI which is to be compared with this. 
	 * 
	 * @return true if the two SipURIs are equal.
	 */
	boolean equals(java.lang.Object o);
	
    /**
     * Returns the value of the specified header. SIP/SIPS URIs may specify
     * headers. As an example, the URI
     * <code>sip:joe@example.com?Priority=emergency</code> has a header
     * "Priority" whose value is "emergency".
     * 
     * @param name  the header name
     * @return the value of the specified header in this <code>SipURI</code>
     */
    String getHeader(String name);
    
    /**
     * Returns an <code>Iterator</code> over the names of all headers
     * present in this <code>SipURI</code>.
     * 
     * @return an <code>Iterator</code> over all header names
     */
    Iterator getHeaderNames();
    
    /**
     * Returns the host part of this <code>SipURI</code>.
     * 
     * @return the host part of this <code>SipURI</code>
     */
    String getHost();
    
    /**
     * Returns true if the "lr" flag parameter is set, and false
     * otherwise.
     * This is equivalent to <code>"".equals(getParameter("lr"))</code>.
     * 
     * @return true if the "lr" flag parameter is set, and false otherwise
     */
    boolean getLrParam();
    
    /**
     * Returns the value of the "maddr" parameter, or null if this
     * is not set.
     * This is equivalent to <code>getParameter("maddr")</code>.
     * 
     * @return the value of the "maddr" parameter
     */
    String getMAddrParam();
    
    /**
     * Returns the value of the "method" parameter, or null if this
     * is not set.
     * This is equivalent to <code>getParameter("method")</code>.
     * 
     * @return the value of the "method" parameter
     */
    String getMethodParam();

    /**
     * Returns the port number of this <code>SipURI</code>,
     * or -1 if this is not set.
     * 
     * @return the port number of this <code>SipURI</code>
     */
    int getPort();
    
    /**
     * Returns the value of the "transport" parameter, or null if this
     * is not set.
     * This is equivalent to <code>getParameter("transport")</code>.
     * 
     * @return the value of the "transport" parameter
     */
    String getTransportParam();
    
    
    /**
     * Returns the value of the "ttl" parameter, or -1 if this
     * is not set. This method is equivalent to
     * <code>getParameter("ttl")</code>.
     * 
     * @return the value of the "ttl" parameter
     */
    int getTTLParam();
    
    /**
     * Returns the user part of this <code>SipURI</code>.
     * 
     * @return the user part of this <code>SipURI</code>
     */
    String getUser();
    
    /**
     * Returns the value of the "user" parameter, or null if this
     * is not set.
     * This is equivalent to <code>getParameter("user")</code>.
     * 
     * @return the value of the "user" parameter
     */
    String getUserParam();
    
    /**
     * Returns the password of this <code>SipURI</code>,
     * or null if this is not set.
     * 
     * @return the password of this <code>SipURI</code>
     */
    String getUserPassword();
    
    /**
     * Returns true if this <code>SipURI</code> is <em>secure</em>, that is,
     * if this it represents a <code>sips</code> URI. For "ordinary"
     * <code>sip</code> URIs, false is returned.
     *
     * @return true if this <code>SipURI</code> represents a <code>sips</code>
     *     URI, and false if it represents a <code>sip</code> URI
     */
    boolean isSecure();

    /**
     * Removes the named header from this SipURI. Nothing is done if the 
     * SipURI did not already contain the specific header.
     *  
     * @param name - header name
     */
    void removeHeader(String name);
    
    /**
     * Sets the value of the specified header in this <code>SipURI</code>.
     * 
     * @param name  header name
     * @param value header value
     */
    void setHeader(String name, String value);
    
    /**
     * Sets the host part of this <code>SipURI</code>. This should be a fully
     * qualified domain name or a numeric IP address.
     * 
     * @param host the new host name
     */
    void setHost(String host);
    
    /**
     * Sets or removes the "lr" parameter depending on the value of the flag.
     * 
     * @param flag specifies that the "lr" flag parameter is to be set (true)
     *        or removed (false)
     */
    void setLrParam(boolean flag);
    
    /**
     * Sets the value of the "maddr" parameter.
     * This is equivalent to <code>setParameter("maddr", maddr)</code>.
     * 
     * @param maddr new value of the "maddr" parameter
     */
    void setMAddrParam(String maddr);
    
    /**
     * Sets the value of the "method" parameter. This specifies which SIP
     * method to use in requests directed at this SIP/SIPS URI.
     * 
     * <p>This method is equivalent to
     * <code>setParameter("method", method)</code>.
     * 
     * @param method new value of the "method" parameter
     */
    void setMethodParam(String method);
    
    /**
     * Sets the port number of this <code>SipURI</code>.
     * 
     * @param port  the new port number. A negative value means the
     *              port number is not set and a subsequent call to
     *              getPort() should return -1.
     */
    void setPort(int port);
    
    /**
     * Sets the scheme of this URI to <code>sip</code> or <code>sips</code>
     * depending on whether the argument is true or not.
     * 
     * @param b determines whether the scheme of this <code>SipURI</code>
     *     is set to <code>sip</code> or <code>sips</code>
     */
    void setSecure(boolean b);

    /**
     * Sets the value of the "transport" parameter. This parameter specifies
     * which transport protocol to use for sending requests and responses to
     * this entity. The following values are defined: "udp", "tcp", "sctp",
     * "tls", but other values may be used also
     * 
     * <p>This method is equivalent to
     * <code>setParameter("transport", transport)</code>.
     * 
     * @param transport new value for the "transport" parameter
     */
    void setTransportParam(String transport);
    
    /**
     * Sets the value of the "ttl" parameter. The ttl parameter specifies
     * the time-to-live value when packets are sent using UDP multicast.
     * 
     * <p>This is equivalent to <code>setParameter("ttl", ttl)</code>.
     * 
     * @param ttl new value of the "ttl" parameter
     */
    void setTTLParam(int ttl);
 
    
    /**
     * Sets the user part of this <code>SipURI</code>.
     * 
     * @param user the new user part
     */
    void setUser(String user);
    
    /**
     * Sets the value of the "user" parameter.
     * This is equivalent to <code>setParameter("user", user)</code>.
     * 
     * @param user new value for the "user" parameter
     */
    void setUserParam(String user);
    
    /**
     * Sets the password of this <code>SipURI</code>. The use of passwords
     * in SIP or SIPS URIs is discouraged as sending passwords in clear text
     * is a security risk.
     * 
     * @param password the new password
     */
    void setUserPassword(String password);
    
    /**
     * Returns the <code>String</code> representation of this
     * <code>SipURI</code>. Any reserved characters will be properly escaped
     * according to RFC2396.
     * 
     * @return this <code>sip</code> or <code>sips</code> URI as a
     *     <code>String</code>
     */
    String toString();
}
