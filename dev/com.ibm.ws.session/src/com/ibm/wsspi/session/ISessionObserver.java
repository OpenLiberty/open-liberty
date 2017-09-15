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

public interface ISessionObserver {

    /**
     * Method sessionCreated
     * <p>
     * 
     * @param session
     */
    public void sessionCreated(ISession session);

    /**
     * Method sessionAccessed
     * <p>
     * 
     * @param session
     */
    public void sessionAccessed(ISession session);

    public void sessionAccessUnknownKey(Object key);

    /**
     * Method sessionDestroyed
     * <p>
     * 
     * @param session
     */
    public void sessionDestroyed(ISession session);

    public void sessionDestroyedByTimeout(ISession session);

    /**
     * Method sessionReleased
     * <p>
     * 
     * @param session
     */
    public void sessionReleased(ISession session);

    /**
     * Method sessionFlushed
     * <p>
     * 
     * @param session
     */
    public void sessionFlushed(ISession session);

    /**
     * Method sessionDidActivate
     * <p>
     * 
     * @param session
     */
    public void sessionDidActivate(ISession session);

    /**
     * Method sessionWillPassivate
     * <p>
     * 
     * @param session
     */
    public void sessionWillPassivate(ISession session);

    /**
     * Method getId
     * <p>
     * 
     * @return String
     */
    public String getId();

    /**
     * @param session
     */
    public void sessionAffinityBroke(ISession session);

    /**
     * @param value
     */
    public void sessionCacheDiscard(Object value);

    /**
     * @param value
     */
    public void sessionLiveCountInc(Object value);

    /**
     * @param value
     */
    public void sessionLiveCountDec(Object value);
    
    /**
     * Method sessionCreated
     * <p>
     * 
     * @param oldId
     * @param session
     */
    public void sessionIdChanged(String oldId, ISession session);

}