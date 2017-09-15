/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JspCoreException extends Exception {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3256999973487720755L;
	protected static ResourceBundle bundleNLS = null;
	protected static ResourceBundle bundleUS = null;
    
	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.JspCoreException";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
    
    static {
        try {
            bundleNLS = ResourceBundle.getBundle("com.ibm.ws.jsp.resources.messages", Locale.getDefault());
        }
        catch (Exception e) {
			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
				logger.logp(Level.WARNING, CLASS_NAME, "static", "Failed to load resource bundle com.ibm.ws.jsp.resources.messages locale "+ Locale.getDefault() +" try default", e);
			}

			try {
				bundleNLS = ResourceBundle.getBundle("com.ibm.ws.jsp.resources.messages", Locale.US);
			}
			catch (Exception e2) {
				if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
					logger.logp(Level.WARNING, CLASS_NAME, "static", "Failed to load resource bundle com.ibm.ws.jsp.resources.messages locale "+ Locale.US, e2);
				}
			}
        }
        if(bundleNLS != null){
			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
				logger.logp(Level.FINEST, CLASS_NAME, "static", "Using resource bundle com.ibm.ws.jsp.resources.messages for locale [" + bundleNLS.getLocale() +"]");
			}
    	}

    }

    protected Object[] args = null;

    public JspCoreException() {
        super();
    }

    public JspCoreException(String messageKey) {
        super(messageKey);
    }

    public JspCoreException(String messageKey, Object[] args) {
        super(messageKey);
        this.args = args;
    }

    public JspCoreException(String messageKey, Throwable exc) {
        super(messageKey, exc);
    }

    public JspCoreException(String messageKey, Object[] args, Throwable exc) {
        super(messageKey, exc);
        this.args = args;
    }

    public JspCoreException(Throwable exc) {
        super(exc==null ? null : exc.toString() , exc);
    }
    
    public String getLocalizedMessage() {
    	// return translated version of the error message
        String result = "";
        try {
            result = bundleNLS.getString(super.getMessage());
            if (args != null)
                result = MessageFormat.format(result, args);
        }
        catch (MissingResourceException e) {
			//	attempt to get message using default resource bundle
            result = getMessage();
        }
        return (result);
    }
    
    public String getMessage (){
    	// return non-translated version of the error message
    	if(bundleUS == null){
    		initUSBundle();
    	}

		String result = "";
		try {
			result = bundleUS.getString(super.getMessage());
			if (args != null)
				result = MessageFormat.format(result, args);
		}
		catch (MissingResourceException e) {
			// return message that was passed into the constructor of this exception (message key).
			result = super.getMessage();
			// 198659 begin
			if(args != null){
				for (int i=0; i < args.length; i++){
					result = result + " [{"+ i+"}" + args[i] +"]";
				}
			}
			// 198659 end
		}
		return (result);
    }
    
    private void initUSBundle(){
		try {
			if(Locale.getDefault() == Locale.US){
				bundleUS = bundleNLS;
			}else{
				bundleUS = ResourceBundle.getBundle("com.ibm.ws.jsp.resources.messages", Locale.US);
			}
		}
		catch (Exception e) {
			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
				logger.logp(Level.WARNING, CLASS_NAME, "initUSBundle", "Failed to load default resource bundle com.ibm.ws.jsp.resources.messages locale "+ Locale.US, e);
			}
		}
    }
    
    
    public static String getMsg(String key) {
        return getMsg(key, null);    
    }
    
    public static String getMsg(String key, Object[] args) {
        String msg = null;
        try {
            msg = bundleNLS.getString(key);
            if (args != null)
                msg = MessageFormat.format(msg, args);
        }
        catch (MissingResourceException e) {
            msg = key;
        }
        return (msg);
    }
}
