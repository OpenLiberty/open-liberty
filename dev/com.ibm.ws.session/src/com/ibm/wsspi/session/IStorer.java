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

/**
 * Analogous to ILoader, IStorer is the "write" side version of interacting
 * with an external store.
 * <p>
 * 
 */
public interface IStorer {

    /**
     * This method is called when a session's UOW has completed and hence
     * any updated state can be sent to the external store (either synchronously
     * or asynchronously). <br>
     * <br>
     * This is equivalent to the call storeSession(session, false);
     * <p>
     * 
     * @param ISession
     *            session: The session to be transferred to the external store
     */
    public void storeSession(ISession session);

    /**
     * This method is called when a session's UOW has completed and hence
     * any updated state can be sent to the external store (either synchronously
     * or asynchronously). <br>
     * 
     * @param ISession
     *            session: The session to be transferred to the external store
     * @param boolean usesCookies: set to true or false depending on whether this
     *        request sessionid
     *        was derived from a cookie or not.
     */
    public void storeSession(ISession session, boolean usesCookies);

    /**
     * If this particular store is sending out updates asynchronously, the
     * interval used to wait between updates can be set with this method. <br>
     * 
     * @param int interval: The time is seconds to wait between sending out of
     *        updates
     */
    public void setStorageInterval(int interval);

}
