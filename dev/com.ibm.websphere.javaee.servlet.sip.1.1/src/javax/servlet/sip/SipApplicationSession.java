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

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

/**
 * Represents application instances. The <code>SipApplicationSession</code>
 * interface acts as a store for application data and provides access to
 * contained <em>protocol</em> sessions, e.g. <code>SipSession</code> and
 * <code>HttpSession</code> objects representing point-to-point signaling
 * relationships.
 */
public interface SipApplicationSession {
	
	
	
	/**
	 * Possible protocols to which sessions contained in 
	 * the SipApplicationSession belong to. 
	 */
	public enum Protocol {
		
		/**
		 * 
		 */
		HTTP,

		/**
		 * 
		 */
		SIP

	}
	
	
    /**
     * 
     * @deprecated. has potential to break application composition instead of this use 
     * the SipApplicationKey mechanism as described in section 15.11.2 in the SIP Servlet 
     * specification document.
     * 
     * Encodes the ID of this <code>SipApplicationSession</code> into the
     * specified URI. The container must then be prepared to associate
     * this application session with an incoming request which was
     * triggered by activating the encoded URI.
     * 
     * <p>In the case of SIP and SIPS URIs, the container may also rewrite
     * the host, port, and transport protocol components of the URI based
     * on its knowledge of local listen points. When doing so it should
     * take existing settings as a hint as to which listen point to select
     * when it has more than one.
     * 
     * <p>This method allow applications to correlate events which would
     * otherwise be treated as being independent, that is, as belonging to
     * different application sessions.  For example, an application might
     * send an instant message with an HTML body to someone. The
     * IM body may then contain a SIP URI pointing back to the SIP servlet
     * container and the 
     * application session in which the IM was generated, thus ensuring
     * that an INVITE triggered by the IM recipient triggering that URI
     * is associated with this application session when received by the
     * container.
     * 
     * <p>Containers are required to support rewriting of SIP and SIPS URIs.
     * 
     * @param uri -  the uri to be encoded
     * @throws IllegalArgumentException if the container doesn't know how
     *      to rewrite the specified URI, for example, because it doesn't
     *      know the specific scheme
     * @throws IllegalStateException if this application session is not valid
     */
    void encodeURI(URI uri) throws IllegalArgumentException, IllegalStateException;
    
    /**
     * Encode specified URL to include the application session ID in a way 
     * such that the parameter used to encode the application session ID 
     * should be unique across implementations. The recommended way is to use 
     * the java package name of the implementation, like com.acme.appsession. 
     * 
     * This mechanism can be used by the applications to encode the HTTP URL with the 
     * application session Id. This URL can then be sent out through some of out of 
     * band mechanism. When the HTTP Request comes back to the converged container 
     * with this request, the container must associate the new HttpSession with the 
     * encoded Application Session. In case the HTTP request is not a new request but 
     * a follow on request already associated with a HTTP Session then the converged 
     * containers must use the HTTP session association mechanism to route the request 
     * to the right HTTP Session. If that HTTP Session was not associated with the encoded 
     * SipApplicationSession in the request then that association MUST occur. 
     * This mechanism is similar to how the (deprecated) encodeURI() operates for SIP.
     *  
     * @param url - the URL to be encoded 
     * @return the resulting URL containing the encoded app session id 
     * @throws IllegalStateException - if this application session is not valid
     */
    URL encodeURL(java.net.URL url) throws IllegalStateException;
    
    
    /**
     * Returns the name of the SIP application this SipApplicationSession 
     * is associated with.
     * 
     * @return name of the SIP application, this SipApplicationSession is 
     * 			associated with
     */
    String getApplicationName();

    /**
     * Returns the object bound with the specified name in this session,
     * or null if no object is bound under the name.
     *
     * @param name  a string specifying the name of the object
     * @return      the object with the specified name
     * @throws IllegalStateException if this application session is not valid
     */
    Object getAttribute(String name) throws IllegalStateException;
    

    /**
     * Returns an <code>Iterator</code> over the <code>String</code>
     * objects containing the names of all the objects bound to this session.
     *
     * @return  an <code>Iterator</code> over the <code>String</code> objects
     *          specifying the names of all the objects bound to this session
     * @throws IllegalStateException if this application session is not valid
     */
    Iterator getAttributeNames() throws IllegalStateException;
    
    /**
     * Returns the time when this session was created, measured in
     * milliseconds since midnight January 1, 1970 GMT.
     *
     * @return  a long specifying when this session was created,
     *          expressed in milliseconds since 1/1/1970 GMT
     * @throws IllegalStateException
     *              if this method is called on an invalidated session
     */
    long getCreationTime() throws IllegalStateException;
    
    /**
     * 
     * Returns the time in future when this SipApplicationSession will expire. 
     * This would be the time of session creation + the expiration time set in 
     * milliseconds. For sessions that are set never to expire, this returns 0. 
     * For sessions that have already expired this returns Long.MIN_VALUE  
     * The time is returned as the number of milliseconds since midnight 
     * January 1, 1970 GMT. 
     * 
     * @return a long representing the time in future when this session will 
     * 			expire expressed in milliseconds since 1/1/1970 GMT. 
     * 
     * @throws IllegalStateException - if this application session is not valid
     */
    long getExpirationTime() throws IllegalStateException;
    
    /**
     * Returns a string containing the unique identifier assigned to
     * this session. The identifier is assigned by the servlet container
     * and is implementation dependent.
     *
     * @return  a <code>String</code> identifier for this application session
     */
    String getId();
    
    /**
     * Returns true if the container will notify the application when this 
     * SipApplicationSession is in the ready-to-invalidate state.
     *  
     * @return - value of the invalidateWhenReady flag 
     * 
     * @throws IllegalStateException - if this application session is not valid
     * 
     * @see SipApplicationSession#isReadyToInvalidate()
     */
    boolean getInvalidateWhenReady() throws IllegalStateException;
    
    /**
     * Returns the last time an event occurred on this application session.
     * For SIP, incoming and outgoing requests and incoming responses are
     * considered events. The time is returned as the number of milliseconds
     * since midnight January 1, 1970 GMT.
     * 
     * <p>Actions that applications take, such as getting or setting a 
     * value associated with the session, do not affect the access time.
     *
     * @return  a long representing the last time the client sent a
     *          request associated with this session, expressed in
     *          milliseconds since 1/1/1970 GMT
     */
    long getLastAccessedTime();

    /**
     * Returns the session object with the specified id associated with the specified 
     * protocol belonging to this application session, or null if not found.
     * 
     * @param id - the session id
     * @param protocol - an Enum identifying the protocol 
     * 
     * @return - the corresponding session object or null if none is found. 
     * 
     * @throws NullPointerException - on null id or protocol
     * @throws IllegalStateException  - if this application session is not valid
     */
    Object getSession(java.lang.String id, 
    		  SipApplicationSession.Protocol protocol) throws NullPointerException, IllegalStateException;
    
    
    /**
     * Returns an <code>Iterator</code> over all "protocol" sessions
     * associated with this application session. This may include a mix
     * of different types of protocol sessions, e.g. <code>SipSession</code>
     * and <code>javax.servlet.http.HttpSession</code> objects.
     * 
     * @return <code>Iterator</code> over set of protocol session belonging
     *          to this application session
     *          
     * @throws IllegalStateException - if this application session is not valid
     * 
     * @throws NullPointerException - on null id or protocol 
     */
    Iterator getSessions() throws IllegalStateException, NullPointerException;
    
    /**
     * Returns an <code>Iterator</code> over the "protocol" session objects
     * associated of the specified protocol associated with this application
     * session. If the specified protocol is not supported, an empty
     * <code>Iterator</code> is returned.
     * 
     * <p>If "SIP" is specified the result will be an <code>Iterator</code>
     * over the set of {@link SipSession} objects belonging to this application
     * session. For "HTTP" the result will be a list of
     * <code>javax.servlet.http.HttpSession</code> objects.
     * 
     * @param protocol a string identifying the protocol name, e.g. "SIP"
     * @return <code>Iterator</code> over protocol sessions of the
     *      specified protocol
     *      
     * @throws IllegalStateException - if this application session is not valid
     * @throws NullPointerException - if the protocol is null 
     * @throws IllegalArgumentException - if the protocol is not understood by container.
     */
    Iterator getSessions(String protocol) throws IllegalStateException, NullPointerException, IllegalArgumentException;
    
    
    /**
     * Returns the SipSession with the specified id belonging to this application 
     * session, or null if not found.
     *  
     * @param id  - the SipSession id 
     * @return Returns the SipSession with the specified id belonging to this 
     * 		   application session, or null if not found. 
     * @throws NullPointerException - on null id
     * @throws IllegalStateException - if this application session is not valid
     */
    SipSession getSipSession(String id) throws NullPointerException, IllegalStateException;
    
    /**
     * Returns the active timer identified by a specific id that is associated 
     * with this application session.
     *  
     * @param id - timer id
     * @return the ServletTimer object identified by the id 
     * 		   belonging to this application session
     *  
     * @throws IllegalStateException - if this application session is not valid
     */
    ServletTimer getTimer(String id) throws IllegalStateException;
    
    /**
     * Returns all active timers associated with this application session.
     * 
     * @return a <code>Collection</code> of <code>ServletTimer</code>
     *     objects belonging to this application session
     *     
     * @throws IllegalStateException if this application session is not valid
     * 
     */
    Collection<ServletTimer> getTimers() throws IllegalStateException;

    /**
     * Invalidates this application session. This will cause any timers
     * associated with this application session to be cancelled.
     * 
     * @throws IllegalStateException if this application session is not valid
     * 
     * @see SipSession#isReadyToInvalidate()
     */
    void invalidate() throws IllegalStateException;
    
    /**
     * Returns true if this application session is in a ready-to-invalidate 
     * state. A SipApplicationSession is in the ready-to-invalidate state 
     * if the following conditions are met:
     * 
     * 1. All the contained SipSessions are in the ready-to-invalidate state.
     * 2. None of the ServletTimers associated with the SipApplicationSession 
     * 		are active
     * 
     * @return true if the application session is in ready-to-invalidate state, 
     * 		   false otherwise 
     * 
     * @throws IllegalStateException
     *
     * @see SipSession#isReadyToInvalidate()
     */
    boolean isReadyToInvalidate() throws IllegalStateException;
    
    /**
     * Returns if this SipApplicationSession is valid, false otherwise. 
     * The SipSession can be invalidated by calling the method {@link SipSession#invalidate()} 
     * on it or if its invalidateWhenReady flag is true and it transitions to the 
     * ready-to-invalidate state. Also the SipSession can be invalidated by the container 
     * when either the associated {@link SipApplicationSession} times out or 
     * {@link SipApplicationSession#invalidate()} is invoked.
     *  
     * @return boolean true if the session is valid, false otherwise.
     */
    boolean isValid();

    /**
     * Removes the object bound with the specified name from this session.
     * If the session does not have an object bound with the specified name,
     * this method does nothing. 
     *
     * @param name - the name of the object to remove from this session
     * 
     * @throws IllegalStateException if this application session is not valid
     */
    void removeAttribute(String name) throws IllegalStateException; 

    /**
     * Binds an object to this session, using the name specified. 
     * If an object of the same name is already bound to the session,
     * the object is replaced.
     * 
     * @param name  the name to which the object is bound; cannot be null
     * @param attribute the object to be bound; cannot be null
     * @throws IllegalStateException if this application session is not valid
     * @throws java.lang.NullPointerException - if the name or attribute is null.
     */
    void setAttribute(String name, Object attribute) throws IllegalStateException;
    
    
    /**
     * Sets the time of expiry for this application session.
     * 
     * <p>This allows servlets to programmatically extend the lifetime
     * of application sessions. This method may be invoked by an
     * application in the notification that the application session has
     * expired: {@link SipApplicationSessionListener#sessionExpired(SipApplicationSessionEvent)}.
     * If the server is willing to extend the session lifetime it returns
     * the actual number of minutes the session lifetime has been extended
     * with, and the listener will be invoked about session expiry again
     * at a later time.
     * 
     * <p>This helps applications clean up resources in a reasonable
     * amount of time in situations where it depends on external events
     * to complete an application session. Being able to extend session
     * lifetime means the application is not forced to choose a very high
     * session lifetime to begin with.
     * 
     * <p>It is entirely up to server policy whether to grant or deny the
     * applications request to extend session lifetime.
     * 
     * Note that any attempt to extend the lifetime of an explicitly
     * invalidated application session, one for which {@link #setExpires}
     * has been invoked, will always fail.
     * 
     * @param deltaMinutes  the number of minutes that the lifetime of this
     *      <code>SipApplicationSession</code> is extended with
     * @return actual number of minutes the lifetime of this session
     *      has been extended with, or 0 if wasn't extended
     *      
     * @throws IllegalStateException if this application session is not valid
     */
    int setExpires(int deltaMinutes) throws IllegalStateException;
    
    /**
     * Specifies whether the container should notify the application when the 
     * SipApplicationSession is in the ready-to-invalidate state as defined above. 
     * The container notifies the application using the 
     * SipApplicationSessionListener.sessionReadyToInvalidate  callback.
     *  
     * @param invalidateWhenReady - if true, the container will observe this application 
     * 		session and notify the application when it is in the ready-to-invalidate state. 
     * 		The application session is not observed if the flag is false. The default is true 
     * 		for v1.1 applications and false for v1.0 applications.
     *  
     * @throws IllegalStateException
     */
    void setInvalidateWhenReady(boolean invalidateWhenReady) throws IllegalStateException;

}
