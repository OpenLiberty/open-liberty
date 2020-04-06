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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;

/**
 * Defines common aspects of SIP requests and responses.
 * 
 * <p>The Servlet API is defined with an implicit assumption that servlets
 * receives requests from clients, inspects various aspects of the
 * corresponding {@link javax.servlet.ServletRequest} object, and generates a
 * response by setting various attributes of a {@link javax.servlet.ServletResponse}
 * object. This model fits HTTP well, because HTTP servlets always execute
 * <em>origin servers</em>; they execute only to process incoming requests
 * and never initiates HTTP requests of their own.
 * 
 * <p>SIP services, on the other hand, does need to be able to initiate
 * requests of their own. This implies that SIP request and response classes
 *  are more symmetric, that is, requests must be writable as well as
 * readable, and likewise, responses must be readable as well as writable.
 * 
 * <p>The <code>SipServletMessage</code> interface defines a number of
 * methods which are common to <code>SipServletRequest</code> and
 * <code>SipServletResponse</code>, for example setters and getters for
 * message headers and content.
 * 
 * <a name="syshdr"><h4>System Headers</h4>
 * 
 * Applications must not add, delete, or modify so-called "system"
 * headers. These are header fields that the servlet container manages:
 * From, To, Call-ID, CSeq, Via, Route (except through
 * <code>pushRoute</code>), Record-Route. Contact is a system header field
 * in messages other than REGISTER requests and responses, as well as 3xx
 * and 485 responses. Additionally, for containers implementing the
 * reliable provisional responses extension, RAck and RSeq are considered
 * system headers also.
 * 
 * <h4>Implicit Transaction State</h4>
 * 
 * <code>SipServletMessage</code> objects always implicitly
 * belong to a SIP transaction, and the transaction state machine
 * (as defined by the SIP specification) constrains
 * what messages can legally be sent at various points of processing. If a
 * servlet attempts to send a message which would violate the SIP
 * specification (for example, the transaction state machine), the container
 * throws an <code>IllegalStateException</code>.
 */
public interface SipServletMessage {
	
	
	/**
	 * 
	 *
	 */
	public enum HeaderForm {
		
		/**
		 * 
		 */
		COMPACT,

		/**
		 * Default container form, also if this is set the indvidual 
		 * headers can be set in different forms. 
		 */
		DEFAULT,

		/**
		 * 
		 */
		LONG;
	}
	
    /**
     * Adds an acceptable <code>Locale</code> of this user agent. The
     * language identified by the <code>Locale</code> will be listed in
     * an Accept-Language header with a lower q-value than any existing
     * Accept-Language value, meaning the locale is less preferred than
     * those already identified in this message.
     * 
     * @param locale    a locale acceptable to this user agent
     */
    void addAcceptLanguage(Locale locale);
    
    /**
     * Adds the specified <code>Address</code> as a new value of the
     * named header field. The address is added as the <em>last</em>
     * header field value.
     * 
     * <p>This method can be used with headers which are defined to contain
     * one or more entries matching
     * <code>(name-addr | addr-spec) *(SEMI generic-param)</code>
     * as defined in RFC 3261. This includes, for example, Contact and Route.
     * 
     * @param name the name of the header to set
     * @param addr the additional address value
     * @param first if true, the address is added as the first value of
     *     the specified header field, otherwise it will be the last
     * @throws IllegalArgumentException if the specified header isn't
     *     defined to hold address values or if the specified header field
     *     is a <a href="#syshdr">system header</a>
     */
    void addAddressHeader(String name, Address addr, boolean first) throws IllegalArgumentException;
    
    /**
     * Adds a header with the given name and value. This method allows
     * headers to have multiple values. The container MAY check that
     * the specified header field can legally appear in the this
     * message.
     *
     * <p>Either the long or compact name can be used to access the header
     * field, as both are treated as equivalent.
     * The list of assigned compact form is available in the IANA registry at http://www.iana.org/assignments/sip-parameters
     * 
     * <p><b>Note:</b> applications should never attempt to set the
     * From, To, Call-ID, CSeq, Via, Record-Route, and Route headers.
     * Also, setting of the Contact header is subject to the constraints
     * mentioned in the <a href="#syshdr">introduction</a>.
     * 
     * @param name  a <code>String</code> specifying the header name, either
     *              the long or compact form
     * @param value the additional header value
     * @throws IllegalArgumentException if the specified header field
     *     is a <a href="#syshdr">system header</a> or if it cannot
     *     legally appear in this message
     */
    void addHeader(String name, String value) throws IllegalArgumentException;
    
    /**
     * Adds the specified Parameterable as a new value of the 
     * named header field. The parameterable is added as the 
     * last  header field value.
     * 
     * This method can be used with headers which are defined to 
     * contain one or more entries matching field-value 
     * *(;parameter-name=parameter-value) as defined in RFC 3261. 
     * 
     * This includes, for example, Event and Via.
     * Either the long or compact name can be used to access the 
     * header field, as both are treated as equivalent.
     *  
     * @param name - the long or compact name of the header to set
     * @param param - the additional parameterable value
     * @param first - if true, the parameterable is added as the first 
     * 				  value of the specified header field, otherwise it will be the last
     *  
     * @throws IllegalArgumentException - if the specified header isn't defined to hold 
     * 						Parameterable values or if the specified header field is a 
     */
    void addParameterableHeader(java.lang.String name,
					            Parameterable param,
					            boolean first) throws IllegalArgumentException ;
    
    /**
     * Returns the preferred <code>Locale</code> that the UA originating
     * this message will accept content in, based on the Accept-Language
     * header. If this message doesn't contain an Accept-Language header,
     * this method returns the default locale for the server.
     * 
     * @return the preferred <code>Locale</code> for the sending user agent
     */
    Locale getAcceptLanguage();
    
    
    ///////////////////////////////////////////////////////////////////
    // header getters/setters
    
    /**
     * Returns an <code>Iterator</code> over <code>Locale</code> objects
     * indicating, in decreasing order starting with the preferred locale,
     * the locales that are acceptable to the sending UA based on the
     * Accept-Language header. If this message doesn't provide an
     * Accept-Language header, this method returns an <code>Iterator</code>
     * containing one <code>Locale</code>, the default locale for the server.
     * 
     * @return  an <code>Iterator</code> over preferred locales for the
     *          UA originating this message
     */
    Iterator getAcceptLanguages();
    
    /**
     * Returns the value of the specified header as a
     * <code>Address</code> object.
     * 
     * <p>This method can be used with headers which are defined to contain
     * one or more entries matching
     * <code>(name-addr | addr-spec) *(SEMI generic-param)</code>
     * as defined in RFC 3261. This includes, for example, Contact and Route.
     * 
     * <p>If there is more than one header field value the first is returned.
     * 
     * @param name a case insensitive <code>String</code> specifying
     *     the name of the header
     * @return value of the header as an <code>Address</code>
     * @throws ServletParseException if the specified header field
     *     cannot be parsed as a SIP address object
     */
    Address getAddressHeader(String name) throws ServletParseException;
    
    /**
     * Returns a <code>ListIterator</code> over all <code>Address</code>
     * header field values for the specified header.
     * 
     * <p>This method can be used with headers which are defined to contain
     * one or more entries matching
     * <code>(name-addr | addr-spec) *(SEMI generic-param)</code>
     * as defined in RFC 3261. This includes, for example, Contact and Route.
     * 
     * <p>Attempts to modify the specified header field through the
     * returned list iterator must fail with an
     * <code>IllegalStateException</code> if the header field is a
     * <a href="#syshdr">system header</a>. For non-system headers the
     * argument to the <code>add</code> and <code>set</code> methods
     * of the iterator returned by <code>getAddressHeaders</code> must
     * be <code>Address</code> objects.
     * 
     * @param name a case insensitive <code>String</code> specifying
     *     the name of the header field
     * @return a <code>ListIterator</code> over the <code>Address</code>
     *     values of the specified header field
     * @throws ServletParseException if the specified header field
     *     cannot be parsed as a SIP address object
     */
    ListIterator getAddressHeaders(String name) throws ServletParseException;
    
    /**
     * Returns the application session to which this message belongs.
     * If the session doesn't already exist it is created.
     * 
     * @return the application session to which this
     *      <code>SipServletMessage</code> belongs
     */
    SipApplicationSession getApplicationSession();
    
    /**
     * Returns the app session to which this message belongs.
     * 
     * @param create    if true the session is created if it didn't
     *                  already exist, otherwise null is returned
     *                  
     * @return  the application session to which this
     *      <code>SipServletMessage</code> belongs
     */
    SipApplicationSession getApplicationSession(boolean create);

    /**
     * Returns the value of the named attribute as an Object, or null if
     * no attribute of the given name exists.
     * 
     * <p>Attributes can be set two ways. The servlet container may set
     *  attributes to make available custom information about a request
     * or a response. For example, for requests made using HTTPS, the
     * attribute <code>javax.servlet.request.X509Certificate</code> can
     * be used to retrieve information on the certificate of the client.
     * Attributes can also be set programatically using
     * {@link #setAttribute(String, Object)}. This allows information to
     * be embedded into a request or response before a
     * {@link javax.servlet.RequestDispatcher} call.
     * 
     * <p>Attribute names should follow the same conventions as package
     * names. Names beginning with <code>javax.servlet.sip.</code> are
     * reserved for definition by the SIP Servlet API.
     * 
     * @param name  a <code>String</code> specifying the name of the attribute
     * 
     * @throws java.lang.NullPointerException - if the name is null.
     * 
     * @return an <code>Object</code> containing the value of the attribute,
     *      or null if the attribute does not exist
     */
    Object getAttribute(String name) throws NullPointerException;

    /**
     * Returns an <code>Enumeration</code> containing the names of the
     * attributes available to this message object. This method returns
     * an empty <code>Enumeration</code> if the request has no attributes
     * available to it.
     * 
     * @return  an <code>Enumeration</code> of strings containing the names
     *          of the request's attributes
     */
    Enumeration getAttributeNames();
    
  
    /**
     * Returns the value of the Call-ID header in this
     * <code>SipServletMessage</code>.
     *
     * @return the Call-ID value of this <code>SipServletMessage</code>
     */
    String getCallId();

    /**
     * <p>Returns the name of the charset used for the MIME body sent in
     * this message. This method returns null if the message does not
     * specify a character encoding.
     * 
     * <p>The message character encoding is used when converting
     * between bytes and characters. If the character encoding hasn't
     * been set explicitly <code>UTF-8</code> will be used for this
     * purpose.
     * 
     * <p>For more information about character encodings and MIME see
     * RFC 2045 (<a href="http://www.ietf.org/rfc/rfc2045.txt">http://www.ietf.org/rfc/rfc2045.txt</a>).
     * 
     * @return  a <code>String</code> specifying the name of the charset,
     *          for example, <code>UTF-8</code>
     */
    String getCharacterEncoding();
    
     /**
     * Returns the content as a Java object. The actual type of the
     * returned object depends on the MIME type of the content itself
     * (the Content-Type). Containers are required to return a
     * <code>String</code> object for MIME type <code>text/plain</code>
     * as for other <code>text/*</code> MIME types for which the
     * container doesn't have specific knowledge.
     *
     * <p>It is encouraged that the object returned for "multipart" MIME
     * content is a {@link javax.mail.Multipart} object.  A byte array
     * is returned for content-types that are unknown to the container.
     * 
     * <p>The message's character encoding is used when the MIME type
     * indicates that the content consists of character data.
     * 
     * <p><b>Note:</b> This method, together with <code>setContent</code>,
     * is modelled over similar methods
     * in the JavaMail API. Whereas the JavaMail API mandates the use of
     * the Java Activation Framework (JAF) as the underlying data handling
     * system, the SIP servlet API doesn't currently require JAF.
     * 
     * @return  an object representing the parsed content, or a
     *          <code>byte[]</code> object containing the raw content
     *          if the MIME type isn't known to the platform
     * @throws IOException   if an <code>IOException</code> occurred
     * @throws UnsupportedEncodingException if the content is textual in
     *     character but this message's character encoding is not
     *     supported by the platform
     */
    Object getContent() throws IOException, UnsupportedEncodingException;
    
    /**
     * Returns the locale of this message. This method returns the
     * <code>Locale</code> identified by the Content-Language header
     * of the message, or, if this is not present, the locale identified
     * by the <em>charset</em> parameter of the Content-Type header.
     * 
     * @return <code>Locale</code> of this message
     */
    Locale getContentLanguage();
    
    /**
     * Returns the length in number of bytes of the content part of this
     * message. This directly reflects the value of the Content-Length header
     * field.
     * 
     * @return an integer containing the length of the request body
     */
    int getContentLength();
    
    /**
     * Returns the value of the Content-Type header field.
     * 
     * @return a <code>String</code> containing the name of the
     *      MIME type of this message, or null if the body is empty
     */
    String getContentType();

    
    ///////////////////////////////////////////////////////////////////
    // body getters/setters
    
    /**
     * Returns the value of the Expires header. The Expires header field
     * gives the relative time after which the message (or content) expires.
     * The unit of measure is seconds.
     * 
     * @return value of Expires header, or -1 if the header does not exist
     */
    int getExpires();
    
    /**
     * Returns the value of the From header.
     * 
     * @return internal representation of the From header
     */
    Address getFrom();
    
    /** 
     * Returns the value of the specified request header as a
     * <code>String</code>.
     * If the request did not include a header of the specified name,
     * this method returns null. If multiple headers exist, the first
     * one is returned. The header name is case insensitive.
     * 
     * @param name a <code>String</code> specifying the header name
     * @return  a <code>String</code> containing the value of the
     *      requested header, or null if the request does not have a
     *      header of that name
     *      
     * @throws java.lang.NullPointerException - if the name is null.     
     */
    String getHeader(String name) throws NullPointerException;
    
    
    /**
     * 
     * @return - the current header form that is on the message. 
     * 			 The default is SipServletMessage#HeaderForm.DEFAULT
     */
    SipServletMessage.HeaderForm getHeaderForm();
    
    /**
     * Returns an <code>Iterator</code> over all the header names this
     * request contains. If the request has no headers, this method
     * returns an empty <code>Iterator</code>.
     * <p><b>Note:</b> This is a fail-fast iterator and can throw ConcurrentModificationException
     * if  the underlying implementation does not allow modification after the 
     * iterator is created. 
     * <p>Some servlet containers do not allow servlets to access headers
     * using this method, in which case this method returns null.
     *
     * @return an <code>Iterator</code> over the names of all header fields
     *      present within this request; if the request has no header fields,
     *      an empty enumeration; if the servlet container does not
     *      allow servlets to use this method, null
     */
    Iterator getHeaderNames();
    
    /**
     * <p>Returns all the values of the specified request header as a
     * ListIterator over a number of <code>String</code> objects. The
     * values returned by the Iterator follow the order in which they
     * appear in the message header.
     *
     * <p>Either the long or compact name can be used to access the header
     * field, as both are treated as equivalent.
     * The list of assigned compact form is available in the IANA registry at http://www.iana.org/assignments/sip-parameters
     *
     * <p>Some headers, such as Accept-Language can be sent
     * by clients as several headers each with a different value rather
     * than sending the header as a comma separated list.
     * 
     * <p>If the request did not include any headers of the specified name,
     * this method returns an empty Iterator. If the request included headers of
     * the specified name with no values, this method returns an Iterator over empty
     * <code>String</code>s. The header name is case
     * insensitive.
     * <p><b>Note:</b> This is a fail-fast iterator and can throw ConcurrentModificationException
     * if  the underlying implementation does not allow modification after the 
     * iterator is created. 
     * <p>Attempts to modify the specified header field through the
     * returned list iterator must fail with an
     * <code>IllegalStateException</code> if the header field is a
     * <a href="#syshdr">system header</a>. 
     *
     * @param name a <code>String</code> specifying the header name, either
     *             the long or compact form
     * @return a <code>ListIterator</code> over the <code>String</code>
     *      values of the specified header field
     *      
     * @throws NullPointerException if the <code>name</code> is null.
     */
    ListIterator getHeaders(String name) throws NullPointerException;
    
    
    /**
     * Returns the IP address of the upstream/downstream hop 
     * from which this message was initially received by the container.
     * 
     * Unlike getRemoteAddr(), this method returns the same value regardless 
     * of which application invokes it in the same application composition 
     * chain of a specific application router.
     *  
     * @return - a String containing the IP address of the sender of this 
     * 			message, or null if it was locally generated
     */
    String getInitialRemoteAddr();
    
    /**
     * Returns the port number of the upstream/downstream hop from which this message 
     * initially received by the container.
     * Unlike getRemotePort(), this method returns the same value regardless of 
     * which application invokes it in the same application composition chain of a 
     * specific application router.
     *  
     * @return - the port number of the sender of this message, or -1 if it was 
     * 			locally generated.
     */
    int getInitialRemotePort();
    
    /**
     * Returns the name of the protocol with which this message was initially 
     * received by the container, e.g. "UDP", "TCP", "TLS", or "SCTP".
     *  
     * @return - name of the protocol this message was initially received with, 
     * 			or null if it was locally generated.
     */
    String getInitialTransport();
    
    /**
     * Returns the domain name or IP address of the interface this
     * message was received on.
     * 
     * @return name of local interface this message was received on,
     *         or null if it was locally generated.
     */
    String getLocalAddr();


    /**
     * Returns the local port this message was received on.
     * 
     * @return local port on which this message was received, or -1
     *         if it was locally generated.
     */
    int getLocalPort();
    
    /**
     * Returns the SIP method of this message. This is a token consisting
     * of all upper-case letters, for example "INVITE". For requests, the
     * SIP method is in the request line while for responses it may be
     * extracted from the CSeq header.
     * 
     * @return the SIP method of this <code>SipServletMessage</code>
     */
    String getMethod();
    
    
    /**
     * Returns the value of the specified header field as a Parameterable object.
     * This method can be used with headers which are defined to contain one or 
     * more entries matching field-value *(;parameter-name=parameter-value) as 
     * defined in RFC 3261. This includes, for example, Event and Via.
     * 
     * Either the long or compact name can be used to access the header field, 
     * as both are treated as equivalent.
     * 
     * If there is more than one header field value the first is returned.
     *  
     * @param name - a case insensitive String specifying the name of the header, 
     * 				 either the long or compact form
     *  
     * @return - value of the header as a Parameterable
     * 
     * @throws     ServletParseException - if the specified header field cannot be 
     * 				parsed as a SIP parameterable object java.lang.NullPointerException 
     *              if the name is null.
     */
    Parameterable getParameterableHeader(java.lang.String name) throws ServletParseException;
    
    
    /**
     * 
     * Returns a ListIterator over all Parameterable  header field values for the specified 
     * header name. The values returned by the Iterator follow the order in which they appear 
     * in the message header.
     * 
     * This method can be used with headers which are defined to contain one or more entries 
     * matching field-value *(;parameter-name=parameter-value) as defined in RFC 3261. 
     * This includes, for example, Event and Via.
     * 
     * Either the long or compact name can be used to access the header field, as both are treated 
     * as equivalent.
     * 
     * If the message did not include any headers of the specified name, this method returns an 
     * empty Iterator. If the message included headers of the specified name with no values, 
     * this method returns an Iterator over empty Strings.
     * 
     * Attempts to modify the specified header field through the returned list iterator 
     * must fail with an IllegalArgumentException if the header field is a system header.
     * 
     * Note: This is a fail-fast iterator and can throw ConcurrentModificationException if 
     * the underlying implementation does not allow modification after the iterator is created. 
     * 
     * @param name - a case insensitive String specifying the name of the header field, either 
     * 				the long or compact form
     *  
     * @return - a ListIterator over the Parameterable  values of the specified header field
     *  
     * @throws ServletParseException     - if the specified header field cannot be parsed as a 
     * 			SIP parameterable object java.lang.NullPointerException - if the name is null.
     */
    ListIterator<? extends Parameterable> getParameterableHeaders(java.lang.String name)
    																throws ServletParseException;    
    
    
    
    /**
     * Returns the name and version of the protocol of this message.
     * This is in the form &lt;protocol> "/" &lt;major-version-number> "."
     * &lt;minor-version-number>, for example "SIP/2.0".
     * 
     * <p>For this version of the SIP Servlet API this is always "SIP/2.0".
     * 
     * @return a <code>String</code> containing the protocol name and
     *      version number
     */
    String getProtocol();
    
    
    ///////////////////////////////////////////////////////////////////
    // Attributes
    
    /**
     * Returns message content as a byte array.
     * 
     * @return  message content as a raw byte array, or null if no content
     *          is set
     * @throws IOException   if an IOException occurred
     */
    byte[] getRawContent() throws IOException;
    
    /**
     * Returns the IP address of the sender of this message.
     *  
     * @return a <code>String</code> containing the IP address of the
     *         sender of this message, or null if it was locally generated
     */
    String getRemoteAddr();
    
    /**
     * Returns the port number of the sender of this message.
     * 
     * @return the port number of the sender of this message, or -1
     *         if it was locally generated.
     */
    int getRemotePort();
    
    ///////////////////////////////////////////////////////////////////
    // Session access
    
    /**
     * Returns the login of the user sending this message, if the
     * user has been authenticated, or null if the user has not been
     * authenticated.
     * 
     * @return a <code>String</code> specifying the login of the user
     *      making this request, or null if the user has not been
     *      authenticated
     */
    String getRemoteUser();
    
    /**
     * Returns the <code>SipSession</code> to which this message belongs.
     * If the session didn't already exist it is created. This method is
     * equivalent to calling <code>getSession(true)</code>.
     * 
     * @return the <code>SipSession</code> to which this
     *      <code>SipServletMessage</code> belongs
     */
    SipSession getSession();
    
    /**
     * Returns the <code>SipSession</code> to which this message belongs.
     * 
     * @param create indicates whether the session is created if it doesn't
     *      already exist
     * @return the <code>SipSession</code> to which this
     *      <code>SipServletMessage</code> belongs, or null if one hasn't
     *      been created and <code>create</code> is false
     */
    SipSession getSession(boolean create);
    
    /**
     * Returns the value of the To header.
     * 
     * @return internal representation of the To header
     */
    Address getTo();
    
    
    ///////////////////////////////////////////////////////////////////
    // Internationalization
    
    /**
     * Returns the name of the protocol with which this message was received,
     * e.g. "UDP", "TCP", "TLS", or "SCTP".
     * 
     * @return name of the protocol this message was received with, or null
     *         if it was locally generated.
     */
    String getTransport();
    
    /**
     * Returns a <code>java.security.Principal</code> object containing
     * the name of the authenticated user agent sending this message.
     * If the user agent has not been authenticated, the method returns null.
     * 
     * @return a <code>java.security.Principal</code> representing the
     *      sending user, or null if the user has not been authenticated
     */
    Principal getUserPrincipal();
    
    /**
     * Returns true if this message is <em>committed</em>, that is, if one
     * of the following conditions is true:
     * <ul>
     * <li>This message is an incoming request for which a final response
     *     has already been generated.
     * <li>This message is an outgoing request which has already been sent.
     * <li>This message is an incoming response received by a servlet acting
     *     as a UAC
     * <li>This message is a response which has already been forwarded upstream
     * </ul>
     * 
     * @return true if this message is committed, false otherwise
     */
    boolean isCommitted();
    
    /**
     * Returns a boolean indicating whether this message was received over
     * a secure channel, such as TLS.
     * 
     * @return a boolean indicating if this message was received over a
     *      secure channel
     */
    boolean isSecure();
    
    /**
     * Returns a boolean indicating whether the authenticated user is
     * included in the specified logical "role". Roles and role
     * membership can be defined using deployment descriptors. If the
     * user has not been authenticated, the method returns false.
     * 
     * @param role  a <code>String</code> specifying the name of the role
     * @return a boolean indicating whether the user sending this message
     *      belongs to a given role; false if the user has not been
     *      authenticated
     */
    boolean isUserInRole(String role);
    
    /**
     * Removes the named attribute from this message. Nothing is done 
     * if the message did not already contain the specified attribute.
     * 
     * Attribute names should follow the same conventions as package names. 
     * Names beginning with javax.servlet.sip.* are reserved for definition by the SIP Servlet API.
     *  
     * @param name - a String specifying the name of the attribute
     *  
     * @throws java.lang.NullPointerException - if name is null.
     */
    void 	removeAttribute(java.lang.String name) throws NullPointerException;
    
    
    /**
     * Removes the specified header. If multiple headers exists with the
     * given name, they're all removed.
     *
     * <p>Either the long or compact name can be used to access the header
     * field, as both are treated as equivalent.
     *
     * @param name  a <code>String</code> specifying the header name, either
     *              the long or compact form
     * @throws IllegalArgumentException if the specified header field
     *     is a <a href="#syshdr">system header</a>
     */
    void removeHeader(String name) throws IllegalArgumentException;
    
    
    ///////////////////////////////////////////////////////////////////
    
    /**
     * Sends this <code>SipServletMessage</code>.
     * 
     * @throws IOException if a transport error occurs when trying to
     *     send this request
     * @throws IllegalStateException if this message cannot legally be sent
     *      in the current state of the underlying SIP transaction
     */
    void send() throws IOException, IllegalStateException;
    
    /**
     * Sets the preferred <code>Locale</code> that this user agent will
     * accept content, reason phrases, warnings, etc. in. The language
     * identified by the <code>Locale</code> will be listed in an
     * Accept-Language header.
     * 
     * <p>A null argument is valid and removes and existing Accept-Language
     * headers.
     * 
     * @param locale    the preferred locale of this user agent
     */
    void setAcceptLanguage(Locale locale);
    
    /**
     * Sets the header with the specified name to have the value
     * specified by the address argument.
     * 
     * <p>This method can be used with headers which are defined to contain
     * one or more entries matching
     * <code>(name-addr | addr-spec) *(SEMI generic-param)</code>
     * as defined in RFC 3261. This includes, for example, Contact and Route.
     * 
     * @param name the name of the header to set
     * @param addr the assigned address value
     * @throws IllegalArgumentException if the specified header isn't
     *     defined to hold address values or if the specified header field
     *     is a <a href="#syshdr">system header</a>
     */
    void setAddressHeader(String name, Address addr) throws IllegalArgumentException;
    
    
    ///////////////////////////////////////////////////////////////////
    // Programmatic security
    
    /**
     * Stores an attribute in this message. Attributes are reset between
     * messages. This method is most often used in conjunction with
     * {@link javax.servlet.RequestDispatcher}.
     * 
     * <p>Attribute names should follow the same conventions as package
     * names. Names beginning with javax.servlet.sip.* are reserved for
     * definition by the SIP Servlet API.
     * 
     * @param name  a <code>String</code> specifying the name of the attribute
     * @param o     the Object to be stored
     * 
     * @throws NullPointerException - if either of name or o is null.
     */
    void setAttribute(String name, Object o) throws NullPointerException;
    
    /**
     * Overrides the name of the character encoding that will be used to
     * convert the body of this message from bytes to characters or vice
     * versa.
     * 
     * <p> Explicitly setting a message's character encoding potentially
     * affects the behavior of subsequent calls to {@link #getContent}
     * and {@link #setContent}.
     * This method must be called prior to calling either of those methods.
     * 
     * @param enc name of the chararacter encoding
     * 
     * @throws java.io.UnsupportedEncodingException - if this is not a valid encoding 
     */
    void setCharacterEncoding(String enc) throws UnsupportedEncodingException;
    
    /**
     * Sets the content of this message to the specified <code>Object</code>.
     * 
     * <p>This method only works if the implementation "knows about" the
     * specified object and MIME type. Containers are requried to handle
     * <code>byte[]</code> content with any MIME type.
     * 
     * <p>Furthermore, containers are required to handle <code>String</code>
     * content
     * when used with a <code>text/*</code> content type. When invoked
     * with non-String objects and a <code>text/*</code> content type,
     * containers may invoke <code>toString()</code> on the content
     * <code>Object</code> in order to obtain the body's character data.
     * It is also recommended that implementations know how to handle
     * {@link javax.mail.Multipart} content when used together with
     * "multipart" MIME types.
     * 
     * <p>When converting <code>String</code> content, this method may
     * use the the message's character encoding
     * (as set by {@link #setCharacterEncoding}, {@link #setContentType}
     * or {@link #setContentLanguage}) to map the <code>String</code> to
     * a byte array.
     * 
     * <p><b>Note:</b> This method, together with
     * {@link #getContent getContent()}, is modelled over a similar method
     * in the JavaMail API. Whereas the JavaMail API mandates the use of
     * the Java Activation Framework (JAF) as the underlying data handling
     * system, the SIP servlet API doesn't currently require JAF.
     * 
     * @param content      an object representing the message content
     * @param contentType  MIME type of the object
     * @throws UnsupportedEncodingException if the content is textual in
     *     nature and this message's character encoding is unsupported by
     *     the server
     * @throws IllegalArgumentException if the platform doesn't know how to
     *          serialize content of the specified MIME type
     * @throws IllegalStateException if the message has already been sent
     *          or if it's read-only
     * @throws UnsupportedEncodingException - if the content is textual in nature 
     * 			and this message's character encoding is unsupported by the server          
     */
    void setContent(Object content, String contentType)
        throws IllegalArgumentException, IllegalStateException, UnsupportedEncodingException;

    
    ///////////////////////////////////////////////////////////////////
    // Transport level information
    
    /**
     * Sets the locale of this message, setting the headers (Content-Language
     * and the Content-Type's charset) as appropriate. This method should
     * be called before a call to <code>setContent</code>.
     * 
     * @param locale the locale of this message
     * 
     * @throws IllegalStateException - if this is an incoming message or if 
     * 								   it has already been sent
     */
    void setContentLanguage(Locale locale) throws IllegalStateException ;
    
    /**
     * Sets the value of the Content-Length header.
     * 
     * <p>Applications are discouraged from setting the Content-Length
     * directly using this method; they should instead use the
     * <code>setContent</code> methods which guarantees that the
     * Content-Length is computed and set correctly.
     * 
     * @param len   an integer specifying the length of the content being
     *      sent to the peer; sets the Content-Length header
     * @throws IllegalStateException if this is an incoming message or if
     *      it has already been sent
     */
    void setContentLength(int len);

    /**
     * Sets the content type of the response being sent to the client.
     * The content type may include the type of character encoding used,
     * for example, <code>text/html; charset=UTF-8</code>. This will
     * cause the message's current character encoding to be set.
     * 
     * <p>If obtaining a <code>PrintWriter</code> or calling
     * <code>setContent</code>, this method should be
     * called first.
     * 
     * @param type  a <code>String</code> specifying the MIME type of
     *              the content
     */
    void setContentType(String type);
    
    /**
     * Sets the value of the Expires header in this message.
     * This method is equivalent to:
     * <pre>
     *   setHeader("Expires", String.valueOf(seconds));
     * </pre>
     * 
     * @param seconds the value of the Expires header measured in seconds
     */
    void setExpires(int seconds);

    /**
     * Sets a header with the given name and value. If the header had
     * already been set, the new value overwrites the previous one. If there
     * are multiple headers with the same name, they all are replaced by this
     * header name, value pair.
     *
     * <p>Either the long or compact name can be used to access the header
     * field, as both are treated as equivalent. The applications choice of 
     * long or compact form shall take effect only of the <code>HeaderForm</code>
     * parameter is set to {@link HeaderForm#DEFAULT}. 
     *
     * <p><b>Note:</b> applications should never attempt to set the
     * From, To, Call-ID, CSeq, Via, Record-Route, and Route headers.
     * Also, setting of the Contact header is subject to the constraints
     * mentioned in the <a href="#syshdr">introduction</a>.
     * 
     * @param name  a <code>String</code> specifying the header name, either
     *              the long or compact form
     * @param value the header value
     * @throws     java.lang.IllegalArgumentException - if the specified header field is a 
     * 													system header 
     * @throws     java.lang.NullPointerException - if the name or value is null
     */
    void setHeader(String name, String value) throws IllegalArgumentException, NullPointerException;
    
    
    /**
     * ndicates which of the compact or long form should the headers in this message 
     * have. If compact is selected then all the headers that have compact names 
     * should be represented with them, regardless of how they were added to the message.
     * When long is selected then all headers change to their long form. 
     * Instead if the applications wish to mix the compact and long form then they must 
     * not invoke the setUseCompactForm method or set it to use 
     * SipServletMessage.HeaderForm.DEFAULT  and instead set the non-system headers 
     * directly using the compact or long form setHeader(String, String). eg.
     * 
     * SipServletMessage message;
     *  .....
     *  message.setHeader("s", "Meeting at 5pm");   // Subject header compact form
     *  message.setHeader("Allow-Events", "telephone-event"); // Long form
     *  .....
     *  
     * 
     * For applications to set each header individually the value of the HeaderForm MUST be 
     * SipServletMessage.HeaderForm.DEFAULT The list of assigned compact form is available in 
     * the IANA registry at http://www.iana.org/assignments/sip-parameters
     *  
     * @param form - desired by the application
     */
    void setHeaderForm(SipServletMessage.HeaderForm form);
    
    
    /**
     * Sets the header with the specified name to have the value specified by 
     * the address argument.
     * 
     * This method can be used with headers which are defined to contain one or 
     * more entries matching field-value *(;parameter-name=parameter-value) as 
     * defined in RFC 3261. This includes, for example, Event and Via.
     * 
     * Either the long or compact name can be used to access the header field, 
     * as both are treated as equivalent. 
     * 
     * @param name
     * @param param
     * @throws java.lang.IllegalArgumentException
     */
    void setParameterableHeader(java.lang.String name,
    							Parameterable param) throws IllegalArgumentException;
    
    
}
