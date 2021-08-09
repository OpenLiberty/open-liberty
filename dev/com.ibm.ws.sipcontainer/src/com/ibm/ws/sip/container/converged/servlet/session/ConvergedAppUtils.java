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
  * Utility class for SIP/HTTP converged application support
  */
public class ConvergedAppUtils {
	
    static private ConvergedAppUtilsInterface _impl;	
	
    /**
      * Retrieve the http session corresponding to the input parms
      * @param virtualHost a string specifying the virtual host of the application
      * @param contextRoot a string specifying the context root of the application
      * @param sessionId a string specifying the session id for the session
      * @return HttpSession
      */
    public static HttpSession getHttpSessionById(String virtualHost, String contextRoot, String sessionId) {
        if (_impl != null) {
            return _impl.getHttpSessionById(virtualHost, contextRoot, sessionId);
        } else { 
    	    return null;
        }
    }

    /**
      * Set a reference to the input IBMApplicationSession into the HttpSession
      * @param sess the HttpSession
      * @param appSess the IBMApplicationSession
      * @return void
      */
    public static void setIBMApplicationSession(HttpSession sess, IBMApplicationSession appSess) {
        if (_impl != null) {
            _impl.setIBMApplicationSession(sess, appSess);
        }
    }
   
    
    /**
     * Create the ConvergedAppUtilsInterface implementation based on the isXD flag  Also
     * sets the application session creator implementation.
     * @param appSessCreator
     * @param isXD - whether the SIP container is using the XD session manager instead of the ND one
     * @return void
     */
    public static void setAppSessCreator(ApplicationSessionCreator appSessCreator) {
       _impl = new BaseConvergedAppUtils();
       _impl.setAppSessCreator(appSessCreator);
    }
}
