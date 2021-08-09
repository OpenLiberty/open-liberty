/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.IStorer;

public class ManualSessionStorer implements IStorer {

    /**
     * Method storeSession
     * <p>
     * 
     * @param session
     * @see com.ibm.wsspi.session.IStorer#storeSession(com.ibm.wsspi.session.ISession)
     */
    public void storeSession(ISession session) {
        session.flush(true); // only cache last accessed times in manual mode
    }

    /**
     * Method storeSession
     * <p>
     * 
     * @param session
     * @param boolean
     * @see com.ibm.wsspi.session.IStorer#storeSession(com.ibm.wsspi.session.ISession)
     */
    public void storeSession(ISession session, boolean usesCookies) {
        session.flush(true); // only cache last acccessed times in manual mode
    }

    /**
     * Method setStorageInterval
     * <p>
     * 
     * @param interval
     * @see com.ibm.wsspi.session.IStorer#setStorageInterval(int)
     */
    public void setStorageInterval(int interval) {

    }

}
