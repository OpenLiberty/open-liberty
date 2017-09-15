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
package com.ibm.ws.webcontainer.util;


import java.security.AccessController;
import java.security.PrivilegedAction;

@SuppressWarnings("unchecked")
public class WebContainerSystemProps {


    private static boolean _sendRedirectCompatibility = false;

    static{
	String doPrivSendRedirect = (String)AccessController.doPrivileged(new PrivilegedAction()
								 {
								     public Object run()
								     {
									 return (System.getProperty("com.ibm.websphere.sendredirect.compatibility"));
								     }
								 });

	if (doPrivSendRedirect!=null)
	{
	    _sendRedirectCompatibility = doPrivSendRedirect.equalsIgnoreCase("true");
	}
    }


    public static boolean getSendRedirectCompatibilty(){
    	return _sendRedirectCompatibility;
    }
}
