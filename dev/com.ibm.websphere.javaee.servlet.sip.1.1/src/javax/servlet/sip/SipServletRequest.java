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

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

/**
 * Represents SIP request messages.  When receiving an incoming SIP request
 * the container creates a <code>SipServletRequest</code> and passes it to
 * the handling servlet. For outgoing, locally initiated requests,
 * applications call SipFactory.createRequest()
 * to obtain a <code>SipServletRequest</code> that can then be modified and
 * sent.
 */
public interface SipServletRequest extends ServletRequest, SipServletMessage {
	
	/**
	 * This method allows the addition of the appropriate authentication 
	 * header(s) to the request that was challenged with a challenge response.
	 *  
	 * @param challengeResponse - The challenge response (401/407) receieved from a UAS/Proxy.
	 * @param authInfo - The AuthInfo object that will add the Authentication 
	 * 					headers to the request.
	 */
	void addAuthHeader(SipServletResponse challengeResponse, AuthInfo authInfo);
	
	
	/**
	 * This method allows the addition of the appropriate authentication header(s) to 
	 * the request that was challenged with a challenge response without needing the creation 
	 * and/or maintenance of the AuthInfo object.
	 *  
	 * @param challengeResponse - the challenge response (401/407) receieved from a UAS/Proxy.
	 * @param username
	 * @param password
	 */
	void addAuthHeader(SipServletResponse challengeResponse,
			           String username,
			           String password);
	
    /**
     * Returns a CANCEL request object. This method is used by
     * applications to cancel outstanding transactions for which they
     * act as a user agent client (UAC). The CANCEL request is sent
     * when the application invokes {@link SipServletRequest#send} on
     * it.
     * 
     * <p>Note that proxy applications MUST use {@link Proxy#cancel()}
     * to cancel outstanding branches.
     * 
     * @return CANCEL request object corresponding to this request
     * @throws IllegalStateException if the transaction state is such that
     *      it doesn't allow a CANCEL request to be sent
     */
    SipServletRequest createCancel() throws IllegalStateException;
    
    /**
     * Creates a response for this request with the specifies status code.
     *
     * @param statuscode    status code for the response
     * @return response object with specified status code
     * @throws IllegalArgumentException if the statuscode is not a valid
     *      SIP status code
     * @throws IllegalStateException    if this request has already been
     *      responded to with a final status code
     */
    SipServletResponse createResponse(int statuscode) 
    			throws IllegalArgumentException, IllegalStateException;
    
    
    /**
     * Creates a response for this request with the specifies status code
     * and reason phrase.
     *
     * @param statusCode    status code for the response
     * @param reasonPhrase  reason phrase to appear in response line
     * @return response object with specified status code and reason phrase
     * @throws IllegalArgumentException if the statuscode is not a valid
     *      SIP status code
     * @throws IllegalStateException    if this request has already been
     *      responded to with a final status code
     */
    SipServletResponse createResponse(int statusCode, String reasonPhrase) 
    			throws IllegalArgumentException, IllegalStateException;
    
    
    /**
     * Returns the B2buaHelper associated with this request. Invocation of this method 
     * also indicates to the container that the application wishes to be a B2BUA, and 
     * any subsequent call to getProxy() will result in IllegalStateException.
     * 
     * @return the B2buaHelper for this request 
     * 
     * @throws IllegalStateException - if getProxy() had already been called
     */
    B2buaHelper getB2buaHelper() throws IllegalStateException;
    
    
    /**
     * If a top route header had been removed by the container upon initially receiving this 
     * request, then this method can be used to retrieve it. Otherwise, if no route header had 
     * been popped then this method will return null.
     * 
     * Unlike getPoppedRoute(), this method returns the same value regardless of which application 
     * invokes it in the same application composition chain.
     * 
     * Note that the URI parameters added to the Record-Route header using Proxy.getRecordRouteURI() 
     * should be retrieved from the URI of the popped route Address using 
     * initialPoppedRoute.getURI().getParameter() and not using initialPoppedRoute.getParameter().
     * 
     * @return Address 
     */
    Address getInitialPoppedRoute();

    /**
     * Always returns null. SIP is not a content transfer protocol and
     * having stream based content accessors is of little utility.
     * 
     * <p>Message content can be retrieved using {@link SipServletMessage#getContent()}
     * and {@link SipServletMessage#getRawContent}.
     * 
     * @return null
     * 
     * @throws IOException
     */
    ServletInputStream getInputStream() throws IOException; 
    
    /**
     * Returns the value of the Max-Forwards header.
     * 
     * @return the value of the Max-Forwards header, or -1 if there is no
     *         such header in this message
     */
    int getMaxForwards();
    
    
    /**
     * If a top route header had been removed by the container upon receiving this 
     * request, then this method can be used to retrieve it. Otherwise, if no route 
     * header had been popped then this method will return null.
     * 
     * Note that the URI parameters added to the Record-Route header using 
     * Proxy.getRecordRouteURI() should be retrieved from the URI of the popped route 
     * Address using poppedRoute.getURI().getParameter() and not using poppedRoute.getParameter().
     * 
     * @return the popped top route header, or null if none
     */
    Address getPoppedRoute();

    /**
     * Returns the <code>Proxy</code> object associated with this request.
     * A <code>Proxy</code> instance will be created if one doesn't already
     * exist. This method behaves the same as <code>getProxy(true)</code>.
     * 
     * <p>Note that the container must return the <em>same</em>
     * <code>Proxy</code> instance whenever a servlet invokes
     * <code>getProxy</code> on messages belonging to the same transaction.
     * In particular, a response to a proxied request is associated with
     * the same <code>Proxy</code> object as is the original request.
     * 
     * <p>This method throws an <code>IllegalStateException</code> if the
     * <code>Proxy</code> object didn't already exist and the transaction
     * underlying this SIP message is in a state which doesn't allow proxying,
     * for example if this is a <code>SipServletRequest</code> for which a
     * final response has already been generated.
     * 
     * @return <code>Proxy</code> object associated with this request
     * @throws TooManyHopsException  if this request has a Max-Forwards
     *      header field value of 0.
     * @throws IllegalStateException if the transaction underlying this
     *      message isn't already associated with a <code>Proxy</code>
     *      object and its state disallows proxying to be initiated,
     *      for example, because a final response has already been generated
     * @see SipServletRequest#getProxy(boolean)
     */
    Proxy getProxy() throws TooManyHopsException;
    
    /**
     * Returns the <code>Proxy</code> object associated with this request.
     * If no <code>Proxy</code> object has yet been created for this request,
     * the <code>create</code> argument specifies whether a <code>Proxy</code>
     * object is to be created or not.
     * 
     * <p>Once a <code>Proxy</code> object has been associated with a request
     * subsequent invocations of this method will yield the same
     * <code>Proxy</code> object, as will the no-argument
     * {@link #getProxy()} method and {@link SipServletResponse#getProxy()}
     * for responses received to proxied requests.
     * 
     * @param create    indicates whether the servlet engine should create
     *      a new <code>Proxy</code> object if one does not already exist
     * @return <code>Proxy</code> object associated with this request
     * @throws TooManyHopsException  if this request has a Max-Forwards
     *      header field value of 0.
     * @throws IllegalStateException if the transaction has already completed
     */
    Proxy getProxy(boolean create) throws TooManyHopsException;

    /**
     * Always returns null. SIP is not a content transfer protocol and
     * having stream based content accessors is of little utility.
     * 
     * <p>Message content can be retrieved using {@link SipServletMessage#getContent()}
     * and {@link SipServletMessage#getRawContent()}.
     * 
     * @return null
     * 
     * @throws IOException
     */
    BufferedReader getReader() throws IOException;
    
    
    /**
     * This method allows the application to obtain the region it was invoked 
     * in for this SipServletRequest. This information helps the application to 
     * determine the location of the subscriber returned by 
     * SipServletRequest.getSubscriberURI().
     * 
     * If this SipServletRequest is an initial request, this method returns the 
     * region in which this servlet is invoked. The SipApplicationRoutingRegion is 
     * only available for initial requests. For all other requests, this method throws 
     * IllegalStateException.
     *  
     * @return The routing region (ORIGINATING, NEUTRAL, TERMINATING or their sub-regions) 
     * 
     * @throws IllegalStateException - if this method is called on a request that is not initial.
     */
    SipApplicationRoutingRegion getRegion() throws IllegalStateException;
    
    /**
     * Returns the request URI of this request.
     * 
     * @return request URI of this <code>SipServletRequest</code>
     */
    URI getRequestURI();
    
    /**
     * Returns the SipApplicationRoutingDirective associated with this request. 
     * @return SipApplicationRoutingDirective associated with this request.
     * @throws IllegalStateException - if called on a request that is not initial
     */
    SipApplicationRoutingDirective getRoutingDirective() throws java.lang.IllegalStateException;


    /**
     * Returns the URI of the subscriber for which this application is invoked to 
     * serve. This is only available if this SipServletRequest received is an initial request. 
     * For all other requests, this method throws IllegalStateException.
     * 
     * @return URI of the subscriber
     * @throws IllegalStateException  - if this method is called on a request that is not initial.
     */
    URI getSubscriberURI() throws IllegalStateException;

    
    
    /**
     * Returns true if this is an <em>initial request</em>. An initial
     * request is one that is dispatched to applications based on the
     * containers configured rule set, as opposed to subsequent requests
     * which are routed based on the <em>application path</em> established
     * by a previous initial request.
     * 
     * @return  true if this is an initial request
     */
    boolean isInitial();
    
    /**
     * Adds a Path header field value to this request. The new value is 
     * added ahead of any existing Path header fields. If this request does 
     * not already contain a Path header, one is added with the value specified 
     * in the argument. This method allows a UAC or a proxy to add Path on a REGISTER Request.
     * 
     * @param uri - The address that is added as a Route header value
     * @throws IllegalStateException - if invoked on non-REGISTER Request.
     */
    void pushPath(Address uri) throws IllegalStateException;
    
    /**
     * Adds a Route header field value to this request with Address argument. 
     * The new value is added ahead of any existing Route header fields. 
     * If this request does not already contains a Route header, one is added 
     * with the value as specified in the argument.
     *
     * This method allows a UAC or a proxy to specify that the request should visit 
     * one or more proxies before being delivered to the destination.
     * 
     * @param uri - the address that is added as a Route header value
     */
    void pushRoute(Address uri);

    /**
     * Adds a Route header field value to this request. The new value is
     * added ahead of any existing Route header fields. If this request does
     * not already container a Route header, one is added with the value
     * specified in the argument.
     * 
     * <p>This method allows a UAC or a proxy to specify that the request
     * should visit one or more proxies before being delivered to the
     * destination.
     * 
     * @param uri  the address that is added as a Route header value
     * 
     * @see "RFC 3261, section 16.6"
     */
    void pushRoute(SipURI uri);
    
    /**
     * Causes this request to be sent. This method is used by SIP servlets
     * acting as user agent clients (UACs) only. Proxying applications use
     * {@link Proxy#proxyTo(URI)} instead.
     *
     * @throws IOException if a transport error occurs when trying to
     *     send this request
     */
    void send() throws IOException;
    
    /**
     * Sets the value of the Max-Forwards header. Max-Forwards serves to
     * limit the number of hops a request can make on the way to its
     * destination. It consists of an integer that is decremented by one
     * at each hop.
     * 
     * <p>This method is equivalent to:
     * <pre>
     *   setHeader("Max-Forwards", String.valueOf(n));
     * </pre>
     * 
     * @param n new value of the Max-Forwards header
     * @throws IllegalArgumentException if the argument is not in the range
     *         0 to 255
     */
    void setMaxForwards(int n) throws IllegalArgumentException;
    
    /**
     * Sets the request URI of this request. This then becomes the
     * destination used in a subsequent invocation of {@link #send send}.
     * 
     * @param uri   new request URI of this <code>SipServletRequest</code>
     * @throws NullPointerException on null <code>uri</code>
     */
    void setRequestURI(URI uri) throws NullPointerException;
    
    /**
     * Sets the application routing directive for an outgoing request.
     * 
     * By default, a request created by SipFactory.createRequest(SipServletRequest 
     * origRequest, boolean sameCallId) continues the application selection process 
     * from origRequest, i.e. directive is CONTINUE. A request created by the other 
     * SipFactory.createRequest() methods starts the application selection process 
     * afresh, i.e. directive is NEW.
     * 
     * This method allows the servlet to assign a routing directive different from the default.
     * 
     * If directive is NEW, origRequest parameter is ignored. If directive is CONTINUE or 
     * REVERSE, the parameter origRequest must be an initial request dispatched by the 
     * container to this application, i.e. origRequest.isInitial() must be true. 
     * 
     * This request must be a request created in a new SipSession or from an initial request, 
     * and must not have been sent. If any one of these preconditions are not met, the method 
     * throws an IllegalStateException.
     * 
     * Note that when a servlet acts as a proxy and calls Proxy.proxyTo() to proxy a request, 
     * the request is always a continuation. 
     * 
     * @param directive - Routing directive
     * @param origRequest - An initial request that the application received 
     * @throws java.lang.IllegalStateException - when given directive cannot be set
     */
    void setRoutingDirective(SipApplicationRoutingDirective directive, SipServletRequest origRequest)
            				throws IllegalStateException;    
  
}
