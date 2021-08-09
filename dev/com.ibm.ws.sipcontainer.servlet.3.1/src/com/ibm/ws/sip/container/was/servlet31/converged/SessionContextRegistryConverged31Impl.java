/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was.servlet31.converged;

import com.ibm.ws.session.SessionApplicationParameters;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.webcontainer.httpsession.SessionManager;
import com.ibm.ws.webcontainer.session.IHttpSessionContext;
import com.ibm.wsspi.sip.converge.ConvergedHttpSessionContextImpl;
import com.ibm.wsspi.sip.converge.SessionContextRegistryConvergedImpl;

/**
 * Extends the SessionContextRegistryConvergedImpl and providing ConvergedApplication (SIP and HTTP)
 * ability.
 * Support for Servlet 3.1 envirnoment.
 */
public class SessionContextRegistryConverged31Impl extends SessionContextRegistryConvergedImpl {

	
	/**
     * @param smgr
     */
    public SessionContextRegistryConverged31Impl(SessionManager smgr) {
        super(smgr);
    }
	 
    /**
    *
    * @see com.ibm.ws.webcontainer.session.impl.SessionContextRegistryImpl#createSessionContextObject(com.ibm.ws.session.SessionManagerConfig, com.ibm.ws.session.SessionApplicationParameters)
    */
   protected IHttpSessionContext createSessionContextObject(SessionManagerConfig smc, SessionApplicationParameters sap)
   {
       return new ConvergedHttpSessionContextImpl(smc, sap, this.smgr.getSessionStoreService());
   } 
}
