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

public interface ISessionStateObserver {

    /**
     * Method sessionAttributeSet
     * <p>
     * 
     * @param session
     * @param key
     * @param oldValue
     * @param newValue
     */
    public void sessionAttributeSet(ISession session, Object key, Object oldValue, Boolean oldIsListener, Object newValue, Boolean newIsListener);

    /**
     * Method sessionAttributeRemoved
     * <p>
     * 
     * @param session
     * @param key
     * @param value
     */
    public void sessionAttributeRemoved(ISession session, Object key, Object value, Boolean oldIsBindingListener);

    // XD methods
    public void sessionAttributeSet(ISession session, Object key, Object oldValue, Object newValue);

    public void sessionAttributeRemoved(ISession session, Object key, Object value);

    /**
     * Method sessionAttributeAccessed
     * <p>
     * 
     * @param session
     * @param key
     * @param value
     */
    public void sessionAttributeAccessed(ISession session, Object key, Object value);

    /**
     * Method sessionUserNameSet
     * <p>
     * 
     * @param session
     * @param oldUserName
     * @param newUserName
     */
    public void sessionUserNameSet(ISession session, String oldUserName, String newUserName);

    /**
     * Method sessionLastAccessTimeSet
     * <p>
     * 
     * @param session
     * @param old
     * @param newaccess
     */
    public void sessionLastAccessTimeSet(ISession session, long old, long newaccess);

    /**
     * Method sessionMaxInactiveTimeSet
     * <p>
     * 
     * @param session
     * @param old
     * @param newval
     */
    public void sessionMaxInactiveTimeSet(ISession session, int old, int newval);

    /**
     * Method sessionExpiryTimeSet
     * <p>
     * 
     * @param session
     * @param old
     * @param newone
     *            TODO Do we even support the setting of a last Expiry time?
     */
    public void sessionExpiryTimeSet(ISession session, long old, long newone);

    /**
     * Method getId
     * <p>
     * 
     * @return String
     */
    public String getId();

}