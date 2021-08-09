/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.shared.util;

import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;

import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * @author todd
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WSFacesUtil {
	
    private static final Logger log = Logger.getLogger("com.ibm.ws.jsf");
	private static final String CLASS_NAME = WSFacesUtil.class.getName();
	
	public static String removeExtraPathInfo (String pathInfo){
		log.logp(Level.FINE, CLASS_NAME, "removeExtraPathInfo", "pathInfo "+ pathInfo);
		if(pathInfo == null)
				return null;
	    int semicolon = pathInfo.indexOf(';');
        if (semicolon != -1){
            pathInfo = pathInfo.substring(0, semicolon);
        }
        if(pathInfo.trim().length() == 0){
        	return null;
        }
        else{
        	log.logp(Level.FINE, CLASS_NAME, "removeExtraPathInfo", "modified pathInfo "+ pathInfo);
        	return pathInfo;
        }
	}
	
    public static ClassLoader getClassLoader(final Object defaultObject)
    {
		ClassLoader cl;
        if (System.getSecurityManager() != null) 
        {
            try {
                cl = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>()
                        {
                            public ClassLoader run() throws PrivilegedActionException
                            {
                                return defaultObject.getClass().getClassLoader();
                            }
                        });
            }
            catch (PrivilegedActionException pae)
            {
                throw new FacesException(pae);
            }
        }
        else
        {
        	cl = defaultObject.getClass().getClassLoader();
        }
        
        return cl;
    }
	
	@SuppressWarnings("unchecked")
	public static ClassLoader getContextClassLoader(IServletContext webapp) throws Exception{
	    ClassLoader classLoader = null;
        if (System.getSecurityManager() != null) {
            final IServletContext _webapp = webapp;
            classLoader = (ClassLoader) AccessController.doPrivileged(new java.security.PrivilegedExceptionAction() {
                public Object run() throws MalformedURLException {
                    return _webapp.getClassLoader();
                }
            });
        }
        else {
            classLoader = webapp.getClassLoader();
        }
        return classLoader;
	}
}
