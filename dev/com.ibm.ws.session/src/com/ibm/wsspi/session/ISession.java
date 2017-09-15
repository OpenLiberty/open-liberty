/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.session;

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

/**
 * This interface defines the view of the session as exposed to the backend
 * store.
 * It does not need to contain any information that is not to be persisted to
 * the
 * outside persistence layer. <br>
 * <br>
 * The only exception to this rule are the adaptations
 * that can be set on an ISession and retrieved from an ISession, but do not
 * have to be persisted to the external store when the session is passivated.
 * These adaptations can be recreated on the fly if absent. The assumption that
 * is
 * being made here is that a session that is actively being used is not going to
 * be passivated.
 * 
 * @author Aditya Desai
 * 
 */

public interface ISession extends Externalizable {

    // XD methods
    /**
     * Returns the multiple protocol correlator
     * 
     * @return
     */
    public Object getCorrelator();

    public Object getAdaptation(Integer protocol);

    public void setAdaptation(Object adaptation, Integer protocol);

    // end of XD methods

    // ----------------------------------------
    // Public Static Members
    // ----------------------------------------

    /*
     * Public constants for use by this class.
     */
    /**
     * Event that indicates that the session was accessed.
     */
    public static final int SESSION_ACCESSED = 1;
    /**
     * Event that indicates that the session was created.
     */
    public static final int SESSION_CREATED = 2;
    /**
     * Event that indicates that the session was destroyed.
     */
    public static final int SESSION_DESTROYED = 3;
    /**
     * Event that indicates that the session was activated in the local jvm.
     */
    public static final int SESSION_ACTIVATED = 4;
    /**
     * Event that indicates that the session was passivated to permanent storage.
     */
    public static final int SESSION_PASSIVATED = 5;
    /**
     * Event that indicates that the session was released post the processing of a
     * request.
     */
    public static final int SESSION_RELEASED = 6;
    /**
     * Event that indicates that the session was flushed to storage.
     */
    public static final int SESSION_FLUSHED = 7;
    /**
     * Event that indicates that the session was destroyed due to timeout.
     */
    public static final int SESSION_DESTROYED_TO = 8;
    /**
     * Event that indicates that the session was accessed in a different jvm from
     * the one that
     * it had affinity to..
     */
    public static final int SESSION_AFFINITY_BROKE = 9;
    /**
     * Event that indicates that the session was discarded from the cache..
     */
    public static final int SESSION_CACHE_DISCARD = 10;
    /**
     * Event that indictates that the sessions in memory has increased
     */
    public static final int SESSION_LIVE_INC = 11;
    /**
     * Event that indictates that the sessions in memory has decreased
     */
    public static final int SESSION_LIVE_DEC = 12;

    public static final int ADAPT_TO_NORMAL_SESSION = 0;
    public static final int ADAPT_TO_APPLICATION_SESSION = 1;

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------

    /**
     * Returns the String id of this session.
     * 
     * @return String
     */
    public String getId();

    /**
     * Returns a true or false depending on whether the session
     * is valid or not. If the session has been invalidated, with the parent
     * ManagedSession still holding a reference to the session, then the
     * session can be queried for validity.
     * 
     * @return boolean
     */
    public boolean isValid();

    /**
     * Returns true if the client does not yet know about the session or if the
     * client chooses not to join the session. For example, if the server used
     * only
     * cookie-based sessions, and the client had disabled the use of cookies, then
     * a session would be new on each request. <br>
     * <br>
     * This method returns a boolean true if this session has been
     * newly created. The isNew boolean will be reset when the
     * session is released (touched in terms of the session)
     * 
     * @return boolean
     */
    public boolean isNew();

    /**
     * This method may be called by the Session Manager to reset the isNew state
     * of a session to false.
     * 
     * @param boolean flag
     */
    public void setIsNew(boolean flag);

    /**
     * This method may be called on the ISession to set the valid flag
     * of the session.
     * 
     * @param boolean flag
     */
    public void setIsValid(boolean flag);

    /**
     * Called on the session to update the last accessed time
     * for this session. It can also be used as the basis to reset the isNew flag.
     */
    public void updateLastAccessTime();

    /**
     * Called on the session to update the last accessed time
     * for this session. It can also be used as the basis to reset the isNew flag.
     */
    public void updateLastAccessTime(long lastAccessTime);

    /**
     * Called on the ISession in order to invalidate the
     * session. This method is used to enforce an explicit user invalidate of
     * the session.
     * 
     */
    public void invalidate();

    /**
     * Called to explicitly flush the state of the session out to
     * the persistent external store.
     * This is equivalent to calling ISession.flush(false);
     * 
     */
    public void flush();

    /**
     * Called to explicitly flush the state of the session out to
     * the persistent external store.
     * 
     * @param metadataOnly
     *            boolean that is set to true if only session metadata
     *            is to be flushed out, not the attributes.
     */
    public void flush(boolean metadataOnly);

    /*
     * Version number related methods
     */
    /**
     * Called to set the version number of this session object.
     * 
     * @param version
     */
    public void setVersion(int version);

    /**
     * Used to retrieve the version number of this session object.
     * 
     * @return int
     */
    public int getVersion();

    /*
     * Adaptation related methods associated with this session
     */
    /**
     * Allows an Adapter to query a session to determine if
     * an adaptation has already been set for later reuse.
     * 
     * @return Object adaptation of this object.
     * @see IProtocolAdapter#adapt(ISession)
     */
    public Object getAdaptation();

    /*
     * Same as getAdaptation except this allows us to pass in a String.
     * Allows us to differentiate between a regular getAdaptation request and an
     * appSession request.
     */
    public Object getAdaptation(int type);

    /**
     * Allows an Adapter to set an Adaptation on an Adaptable
     * session for reuse in the future.
     * 
     * @param adaptation
     */
    public void setAdaptation(Object adaptation);

    /*
     * Allows us to set the specific adaptation (either the appSession adaptation
     * or the normal one)
     */
    public void setAdaptation(Object adaptation, int i);

    /*
     * Getter and Setter for the Context ID
     */

    /**
     * Getter for the context id that identifies the servlet context.
     * 
     * @return String
     */
    public String getContextId();

    /**
     * Setter for the context id.
     * 
     * @param contextID
     */
    public void setContextId(String contextID);

    /*
     * Getter and Setter for the User Name
     */

    /**
     * Getter for the user name associated with this session.
     * 
     * @return String
     */
    public String getUserName();

    /**
     * Setter for the user name associated with this session.
     * 
     * @param userName
     */
    public void setUserName(String userName);

    /**
     * Method isOverflow;
     * <p>
     * Getter for the overflow indicator associated with this session.
     * 
     * @return boolean
     */
    public boolean isOverflow();

    /**
     * Method setOverflow();
     * <p>
     * Setter for the overflow indicator associated with this session to true
     */
    public void setOverflow();

    /*
     * Time related methods associated with this session
     */

    /**
     * Getter for the creationTime for this session.
     * 
     * @return long
     */
    public long getCreationTime();

    /**
     * Getter for the lastAccessedTime for this session in
     * milliseconds.
     * 
     * @return long
     */
    public long getLastAccessedTime(); // per spec, time of previous request

    /**
     * Getter for the time of the current request
     * 
     * @return long
     */
    public long getCurrentAccessTime(); // time of current request

    /**
     * Returns the maxInactiveInterval of this session.
     * This int remains constant through the life of the session, so long
     * as its value is not modified by the application. Unit is in seconds.
     * 
     * @return int
     */
    public int getMaxInactiveInterval();

    /**
     * Sets the MaxInactiveInterval in seconds.
     * 
     * @param maxInactiveInterval
     */
    public void setMaxInactiveInterval(int maxInactiveInterval);

    /**
     * Returns the expiry time of this session in milliseconds.
     * This number is a computed number given the lastAccessedTime and
     * the maxInactiveInterval of this session.
     * 
     * @return long
     */
    public long getExpiryTime();

    /**
     * Method used to set the expiry time of this session
     * 
     * @param l
     */
    public void setExpiryTime(long l);

    /*
     * Attributes related methods associated with this session
     */
    /**
     * Returns an attribute value, given its name.
     * 
     * @param name
     * @return Object
     */
    public Object getAttribute(Object name);

    /**
     * Allows the user of this session to set an attribute
     * by giving its name and value.
     * 
     * @param name
     * @param value
     * @param isListener
     *            boolean true or false depending on whether this attribute is a
     *            listener or not.
     * @return Object
     */
    public Object setAttribute(Object name, Object value, Boolean isListener);

    /**
     * Removes an attribute from a session, given its name.
     * 
     * @param name
     * @return Object
     */
    public Object removeAttribute(Object name);

    /**
     * This method returns a map of all the attributes associated with this
     * session.
     * Depending on the backend store, this could be a rather expensive method
     * call - especially if all the attributes are not required. Use the
     * getAttributeNames method instead as far as possible.
     * 
     * @return Map
     */
    public Map getAttributes();

    /**
     * This method returns an enumeration of attributeNames associated with
     * this session.
     * 
     * @return Enumeration
     */
    public Enumeration getAttributeNames();

    /**
     * This method will return a ArrayList of the names of all the attributes
     * contained in this session that have a listener associated with it.
     * 
     * @return ArrayList
     */
    public ArrayList getListenerAttributeNames();

    /**
     * This method returns a boolean true or false depending on
     * whether this session has attribute listeners associated with it.
     * 
     * @return boolean
     */
    public boolean hasAttributeListeners();

    /**
     * This method returns the number of attribute listeners
     * associated with this session.
     * 
     * @return int
     */
    /*
     * TODO (Aditya) I believe that a getListenerAttributes is a more
     * useful call as compared to the get and set Attribute listener
     * count methods.
     */
    public int getAttributeListenerCount();

    /**
     * Allows the user to set the number of listeners
     * associated with this session.
     * 
     * @param attributeListenerCount
     */
    public void setAttributeListenerCount(int attributeListenerCount);

    /**
     * Allows the IStore to see if it has already set a
     * transactional or lock context on this particular session, and retrieve
     * it if need be
     * 
     * @return lockContext
     */
    public Object getLockContext();

    /**
     * Allows the IStore to set whatever transactional
     * or lock context it is using to manage updates for this particular
     * session.
     * 
     * @param lockContext
     * 
     */
    public void setLockContext(Object lockContext);

    /*
     * RefCount related methods
     */
    /**
     * This method is called to increment the refCount
     */
    public int incrementRefCount();

    /**
     * This method is called to decrement the refCount
     */
    public int decrementRefCount();

    /**
     * This method is called to set the refCount (to be used to reset the variable
     * only in unique cases)
     */
    public void setRefCount(int i);

    /**
     * Getter for the number of references to the session
     * 
     * @return int
     */
    public int getRefCount();

    /**
     * To enable/disable sharing across web apps
     * 
     * @param flag
     */
    public void setSharingAcrossWebapps(boolean flag);

    /**
     * Getter to get the current Session's store instance
     * 
     * @return IStore
     */
    public IStore getIStore();
    
    
    public void setId(String id);

}