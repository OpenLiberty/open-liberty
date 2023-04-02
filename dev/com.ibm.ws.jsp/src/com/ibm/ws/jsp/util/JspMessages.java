/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class JspMessages {
    private static ResourceBundle NLS_BUNDLE; 
    static {
        try{
            NLS_BUNDLE = ResourceBundle.getBundle("com.ibm.ws.jsp.resources.messages", Locale.getDefault());
        } catch (Exception e){
            NLS_BUNDLE = ResourceBundle.getBundle("com.ibm.ws.jsp.resources.messages");
        }
    }
  
    public static String getMessage(String key){
        return getMessage (key, null);
    }
  
    public static String getMessage(String key, Object[] args) {
        String msg = null;
        try {
            msg = NLS_BUNDLE.getString(key);
            if (args != null)
                msg = MessageFormat.format(msg, args);
        } catch (MissingResourceException e) {
            msg = key;
        }
        return (msg);
    }
}
