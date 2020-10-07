/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.converged.servlet.session;

import javax.servlet.http.HttpSession;

import com.ibm.websphere.servlet.session.IBMApplicationSession;
import com.ibm.ws.session.HttpSessionFacade;
import com.ibm.ws.session.SessionContextRegistry;
import com.ibm.ws.sip.container.converged.session.ApplicationSessionCreator;
import com.ibm.wsspi.sip.converge.ConvergedHttpSessionImpl;

/**
 * Implements the ConvergedAppUtilsInterface API 
 * @see com.ibm.ws.sip.container.converged.servlet.session.ConvergedAppUtilsInterface
 */
public class BaseConvergedAppUtils implements ConvergedAppUtilsInterface {

    public BaseConvergedAppUtils() {
    }

    /**
     * @see com.ibm.ws.sip.container.converged.servlet.session.ConvergedAppUtilsInterface#getHttpSessionById(java.lang.String, java.lang.String, java.lang.String)
     */
    public HttpSession getHttpSessionById(String virtualHost,String contextRoot, String sessionId) {
        return SessionContextRegistry.getInstance().getHttpSessionById(virtualHost, contextRoot, sessionId);
    }

    /**
     * @see com.ibm.ws.sip.container.converged.servlet.session.ConvergedAppUtilsInterface#setIBMApplicationSession(javax.servlet.http.HttpSession, com.ibm.websphere.servlet.session.IBMApplicationSession)
     */
    public void setIBMApplicationSession(HttpSession sess, IBMApplicationSession appSess) {
        ((HttpSessionFacade)sess).setIBMApplicationSession(appSess);
    }

    /** 
     *  @see com.ibm.ws.sip.container.converged.servlet.session.ConvergedAppUtilsInterface#setAppSessCreator(com.ibm.ws.sip.container.converged.session.ApplicationSessionCreator)
     */
    public void setAppSessCreator(ApplicationSessionCreator appSessCreator) {
        ConvergedHttpSessionImpl.setAppSessCreator(appSessCreator);
    }
}
