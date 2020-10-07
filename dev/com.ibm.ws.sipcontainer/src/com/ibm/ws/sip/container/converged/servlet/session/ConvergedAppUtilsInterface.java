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
import com.ibm.ws.sip.container.converged.session.ApplicationSessionCreator;

/**
 * Defines API used by converged application to store and correlate 
 * between SIP and HTTP sessions.
 */
public interface ConvergedAppUtilsInterface {
    /**
     * Retrieve the http session corresponding to the input parms
     * @param virtualHost a string specifying the virtual host of the application
     * @param contextRoot a string specifying the context root of the application
     * @param sessionId a string specifying the session id for the session
     * @return HttpSession
     */
   public HttpSession getHttpSessionById(String virtualHost, String contextRoot, String sessionId);

    /**
     * Set a reference to the input IBMApplicationSession into the HttpSession
     * @param sess the HttpSession
     * @param appSess the IBMApplicationSession
     * @return void
     */
   public void setIBMApplicationSession(HttpSession sess, IBMApplicationSession appSess); 
   
   /**
    * Sets the ApplicationSessionCreator implementation
    * @param appSessCreator
    * @return void
    */
   public void setAppSessCreator(ApplicationSessionCreator appSessCreator);
  
}
