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
package com.ibm.ws.sip.container.converged.session;

import com.ibm.websphere.servlet.session.IBMApplicationSession;
import com.ibm.websphere.servlet.session.IBMSession;

/**
 * ApplicationSessionCreator - this interface is implemented by SIP so that
 * session manager can call into the SIP container to get/create a 
 * SIP/IBM ApplicationSession
 */
public interface ApplicationSessionCreator {
    
	/**
	 * Create IBMApplicationSession
	 */
	public IBMApplicationSession createApplicationSession(IBMSession httpSession,String appName, String pathInfo);

        
	/**
	 * @see javax.servlet.http.HttpSession.ConvergedHttpSession#encodeURL(java.lang.String)
	 */
    public String encodeURL(String url);
    
    
    /**
	 * @see javax.servlet.http.HttpSession.ConvergedHttpSession#encodeURL(java.lang.String, java.lang.String)
	 */
    public String encodeURL(String relativePath, String scheme);

}
