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
package com.ibm.ws.jsf.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FacesMessages {
    protected static ResourceBundle bundle = null;
    
	//	Log instance for this class
    protected static final Logger logger = Logger.getLogger("com.ibm.ws.jsf");
    private static final String CLASS_NAME="com.ibm.ws.jsf.util.FacesMessages";
    
    static {
        try {
            bundle = ResourceBundle.getBundle("com.ibm.ws.jsf.resources.messages", Locale.getDefault());
        }
        catch (Exception e) {
			if(logger.isLoggable(Level.WARNING)){
				logger.logp(Level.WARNING, CLASS_NAME, "static", "Failed to load resource bundle com.ibm.ws.jsf.resources.messages locale "+ Locale.getDefault(), e);
			}
        }
        if(bundle == null){
            try {
                bundle = ResourceBundle.getBundle("com.ibm.ws.jsf.resources.messages", Locale.US);
            }
            catch (Exception e) {
                if(logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "static", "Failed to load default resource bundle com.ibm.ws.jsf.resources.messages locale "+ Locale.US, e);
                }
            }
        }
    }

    public static String getMsg(String key) {
        return getMsg(key, null);    
    }
    
    public static String getMsg(String key, Object[] args) {
        String msg = null;
        try {
            msg = bundle.getString(key);
            if (args != null)
                msg = MessageFormat.format(msg, args);
        }
        catch (MissingResourceException e) {
            msg = key;
        }
        return (msg);
    }
}
