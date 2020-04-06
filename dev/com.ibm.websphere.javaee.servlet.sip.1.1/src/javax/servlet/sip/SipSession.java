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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;


/**
 * Represents point-to-point SIP relationships. It roughly corresponds
 * to a SIP dialog. In particular, for UAs it maintains (or is otherwise
 * associated with) dialog state so as to be able to create subequent
 * requests belonging to that dialog (using {@link #createRequest
 * createRequest}).
 * 
 * <p>For UACs, <code>SipSession</code> extend the notion of SIP dialogs
 * to have well-defined state before a dialog has been established and
 * after a final non-2xx terminates an early dialog. This allows UACs to
 * create "subsequent" requests without having an established dialog. The
 * effect is that the subsequent request will have the same Call-ID,
 * From and To headers (with the same From tag and without a To tag),
 * the will exist in the same CSeq space.
 * 
 * <p>All messages are potentially associated with a <code>SipSession</code>.
 * The <code>SipSession</code> can be retrieved from the message by calling
 * {@link SipServletMessage#getSession()}. 
 */
public interface SipSession {
	
	
	
	/**
	 * Possible SIP dialog states from SipSession FSM. 
	 */
	public enum State {
		
		/**
		 * 
		 */
		CONFIRMED ,

		/**
		 * 
		 */
		EARLY, 

		/**
		 * 
		 */
		INITIAL, 
		
		/**
		 * 
		 */
		TERMINATED 
	}
	
	
    /**
     * Returns a new request object. This method is used by user
     * agents only.
     * 
     * <p>Note that this method must not be used to create ACK or
     * CANCEL requests. User agents create ACKs by calling {@link
     * SipServletResponse#createAck} and CANCELs are created by
     * calling {@link SipServletRequest#createCancel}.
     * 
     * @param method    the SIP method of the new request
     * @return          the new request object
     * @throws IllegalArgumentException
     *                  if <code>method</code> is not a syntactically valid
     *                  SIP method or if it's "ACK" or "CANCEL"
     * @throws IllegalStateException
     *                  if this <code>SipSession</code> has been invalidated
     */
    SipServletRequest createRequest(String method) throws IllegalArgumentException, 
    													  IllegalStateException ;

    /**
     * Returns the application session with which this <code>SipSession</code>
     * is associated.
     * 
     * @return the application session for this <code>SipSession</code>
     */
    SipApplicationSession getApplicationSession();

    /**
     * Returns the object bound with the specified name in this session,
     * or null if no object is bound under the name.
     *
     * @param name  a string specifying the name of the object
     * @return      the object with the specified name
     * @throws IllegalStateException
     *              if this method is called on an invalidated session
     * @throws NullPointerException  - if the name is null.
     */
    Object getAttribute(String name) throws IllegalStateException, NullPointerException;

    /**
     * Returns an <code>Enumeration</code> over the <code>String</code>
     * objects containing the names of all the objects bound to this session.
     *
     * @return  an Enumeration over the String objects specifying the names
     *          of all the objects bound to this session
     * @throws IllegalStateException
     *              if this method is called on an invalidated session
     */
    Enumeration getAttributeNames() throws IllegalStateException ;

    
    /**
     * Returns the Call-ID for this <code>SipSession</code>. This is the
     * value of the Call-ID header for all messages belonging to this session.
     *
     * @return the Call-ID for this <code>SipSession</code>
     */
    String getCallId();
    
    /**
     * Returns the time when this session was created, measured in
     * milliseconds since midnight January 1, 1970 GMT.
     *
     * @return  a long specifying when this session was created,
     *          expressed in milliseconds since 1/1/1970 GMT
     * @throws IllegalStateException
     *              if this method is called on an invalidated session
     */
    long getCreationTime();
    
    /**
     * Returns a string containing the unique identifier assigned to
     * this session. The identifier is assigned by the servlet container
     * and is implementation dependent.
     *
     * @return  a string specifying the identifier assigned to this session
     */
    String getId();
    

    /**
     * Returns true if the container will notify the application when this 
     * SipSession is in the ready-to-invalidate state.
     *  
     * @return - value of the invalidateWhenReady flag 
     * @throws IllegalStateException - if this method is called on an invalidated session
     * 
     * @see SipSession#isReadyToInvalidate()
     */
    boolean getInvalidateWhenReady() throws IllegalStateException;
    
    
    /**
     * Returns the last time the client sent a request associated with this
     * session, as the number of milliseconds since midnight January 1,
     * 1970 GMT.
     * 
     * Actions that your application takes, such as getting or setting a 
     * value associated with the session, do not affect the access time.
     *
     * @return  a long representing the last time the client sent a
     *          request associated with this session, expressed in
     *          milliseconds since 1/1/1970 GMT
     */
    long getLastAccessedTime();
    
    /**
     * Returns the <code>Address</code> identifying the local party. This is
     * the value of the From header of locally initiated requests in this leg.
     * 
     * @return address of local party
     */
    Address getLocalParty();
    
    /**
     * 
     * This method allows the application to obtain the region it was 
     * invoked in for this SipSession. This information helps the application 
     * to determine the location of the subscriber returned by {@link SipSession#getSubscriberURI()}.
     * 
     * If this SipSession is created when this servlet receives an initial request, 
     * this method returns the region in which this servlet is invoked. The 
     * SipApplicationRoutingRegion is only available if this SipSession received an initial 
     * requestx. Otherwise, this method throws IllegalStateException. 
     * 
     * @return The routing region (ORIGINATING, NEUTRAL, 
     * 			TERMINATING or their sub-regions)
     *  
     * @throws IllegalStateException - if this method is called on an invalidated session
     */
    SipApplicationRoutingRegion getRegion() throws IllegalStateException;
    
    
    /**
     * Returns the <code>Address</code> identifying the remote party. This is
     * the value of the To header of locally initiated requests in this leg.
     * 
     * @return remote party address
     */
    Address getRemoteParty();
    
    /**
     * Returns the ServletContext to which this session belongs. By definition, 
     * there is one ServletContext per sip (or web) module per JVM. Though, a 
     * SipSession belonging to a distributed application deployed to a distributed 
     * container may be available across JVMs , this method returns the context that 
     * is local to the JVM on which it was invoked.
     *  
     * @return ServletContext object for the sip application
     */
    ServletContext getServletContext();
    
    
    /**
     * Returns the current SIP dialog state, which is one of INITIAL, EARLY, CONFIRMED, or 
     * TERMINATED. These states are defined in RFC3261.
     *  
     * @return the current SIP dialog state
     *  
     * @throws IllegalStateException
     */
    SipSession.State getState() throws IllegalStateException;
    
    /**
     * Returns the URI of the subscriber for which this application is invoked to serve. 
     * This is only available if this SipSession received an initial request. Otherwise, 
     * this method throws IllegalStateException. 
     * 
     * @return URI of the subscriber 
     * 
     * @throws IllegalStateException
     */
    URI getSubscriberURI() throws IllegalStateException ;
    
    /**
     * Invalidates this session and unbinds any objects bound to it.
     * 
     * @throws IllegalStateException
     *              if this method is called on an invalidated session
     */
    void invalidate() throws IllegalStateException ;

    /**
     * Returns true if this session is in a ready-to-invalidate state. A SipSession 
     * is in the ready-to-invalidate state under any of the following conditions:
     * 
     *    1. The SipSession transitions to the TERMINATED state.
     *    2. The SipSession transitions to the COMPLETED state when it is acting as a 
     *    non-record-routing proxy.
     *    3. The SipSession acting as a UAC transitions from the EARLY state back to 
     *    the INITIAL state on account of receiving a non-2xx final response and 
     *    has not initiated any new requests (does not have any pending transactions).
     *          
     *    @return true if the session is in ready-to-invalidate state, false otherwise 
     *    
     *    @throws IllegalStateException  - if this method is called on an invalidated session
     */
    boolean isReadyToInvalidate() throws IllegalStateException;
    
    /**
     * Returns true if this SipSession is valid, false otherwise. The SipSession can be 
     * invalidated by calling the method invalidate() on it or if its invalidateWhenReady 
     * flag is true and it transitions to the ready-to-invalidate state. Also the SipSession 
     * can be invalidated by the container when either the associated SipApplicationSession 
     * times out or SipApplicationSession.invalidate() is invoked.
     *  
     * @return boolean true if the session is valid, false otherwise.
     */
    boolean isValid();
    
    /**
     * Removes the object bound with the specified name from this session.
     * If the session does not have an object bound with the specified name,
     * this method does nothing. 
     *
     * @param name  the name of the object to remove from this session
     * @throws IllegalStateException
     *              if this method is called on an invalidated session
     */
    void removeAttribute(String name) throws IllegalStateException;

    /**
     * Binds an object to this session, using the name specified. 
     * If an object of the same name is already bound to the session,
     * the object is replaced.
     * 
     * @param name  the name to which the object is bound; cannot be null
     * @param attribute the object to be bound; cannot be null
     * @throws IllegalStateException
     *              if this method is called on an invalidated session
     */
    void setAttribute(String name, Object attribute) throws IllegalStateException;

    /**
     * Sets the handler for this <code>SipSession</code>.
     * This method can be used to
     * explicitly specify the name of the servlet which should handle all
     * subsequently received messages for this <code>SipSession</code>.
     * The servlet must belong to the same application (i.e. same
     * <code>ServletContext</code>) as the caller.
     * 
     * @param name of the servlet to be invoked for incoming
     *      SIP messages belonging to this <code>SipSession</code>
     * @throws ServletException if no servlet with the specified name
     *      exists in this application
     *  
     * @throws IllegalStateException  - if this method is called on an invalidated session     
     *      
     */
    void setHandler(String name) throws ServletException, IllegalStateException;
    
    /**
     * Specifies whether the container should notify the application when the SipSession is in 
     * the ready-to-invalidate state as defined above. The container notifies the application 
     * using the SipSessionListener.sessionReadyToInvalidate callback.
     *  
     * @param invalidateWhenReady - if true, the container will observe this session and notify 
     * 	the application when it is in the ready-to-invalidate state. The session is not observed 
     * 	if the flag is false. The default is true for v1.1 applications and false for v1.0 
     * 	applications. 
     * 
     * @throws IllegalStateException
     * 
     * @see     SipSession#isReadyToInvalidate() 
     * @see     SipSessionListener#sessionReadyToInvalidate(SipSessionEvent se)
     */
    void setInvalidateWhenReady(boolean invalidateWhenReady) throws IllegalStateException ;
    

	
	/**
	 * In multi-homed environment this method can be used to select the outbound 
	 * interface to use when sending requests for this SipSession. The specified 
	 * address must be the address of one of the configured outbound interfaces. 
	 * The set of SipURI objects which represent the supported outbound interfaces 
	 * can be obtained from the servlet context attribute named 
	 * javax.servlet.sip.outboundInterfaces.
	 * 
	 * Invocation of this method also impacts the system headers generated by the 
	 * container for this message, such as the the Via and the Contact header. 
	 * The supplied IP address is used to construct these system headers. 
	 * 
	 * @param address - the address which represents the outbound interface
	 *  
	 * @throws IllegalStateException    - if this method is called on an invalidated session  
	 * @throws IllegalArgumentException - if the uri is not understood by the container as one of its
	 *             				   outbound interface
	 * @throws NullPointerException     - on null address
	 *               
	 */
	void setOutboundInterface(InetAddress address) throws	IllegalStateException,	
																	IllegalArgumentException,	
																	NullPointerException;
    
    
    
	/**
	 * In multi-homed environment this method can be used to select the outbound 
	 * interface to use when sending requests for this SipSession. The 
	 * specified address must be the address of one of the configured outbound 
	 * interfaces. The set of SipURI objects which represent the supported outbound 
	 * interfaces can be obtained from the servlet context attribute named 
	 * javax.servlet.sip.outboundInterfaces.
	 * 
	 * Invocation of this method also impacts the system headers generated by the container 
	 * for this message, such as the the Via and the Contact header. The supplied IP 
	 * address is used to construct these system headers.  
	 * 
	 * @param address - the address which represents the outbound interface
	 *  
	 * @throws IllegalStateException    - if this method is called on an invalidated session  
	 * @throws IllegalArgumentException - if the uri is not understood by the container as one of its
	 *             				   outbound interface
	 * @throws NullPointerException     - on null address
	 *               
	 */
	void setOutboundInterface(InetSocketAddress address) throws	IllegalStateException,	
																	IllegalArgumentException,	
																	NullPointerException;
	
    
}
