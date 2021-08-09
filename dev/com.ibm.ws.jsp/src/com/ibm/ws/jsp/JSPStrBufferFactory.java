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
// Change History
// Create for defect  347278
package com.ibm.ws.jsp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JSPStrBufferFactory {
    static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.JSPStrBufferFactory";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    static Class buf=null;
    
    static public void set(Class buffer){
    	buf=buffer;
    }
    static public synchronized JSPStrBuffer getJSPStrBuffer() {
    	JSPStrBuffer strBuf=null;
		if (buf!=null) {
			try {
				strBuf = (JSPStrBuffer)buf.newInstance();
			} catch (Exception e) {
		        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)) {
		            logger.logp(Level.WARNING, CLASS_NAME, "getJSPStrBuffer", "unable to create instance of ["+buf.getName()+"]");
			        StringWriter sw = new StringWriter();
			        PrintWriter pw = new PrintWriter(sw);
			        e.printStackTrace(pw);
			        pw.flush();
			        String stackTrace=sw.toString();
		            logger.logp(Level.WARNING, CLASS_NAME, "getJSPStrBuffer", "stack trace: ["+stackTrace+"]");
		            logger.logp(Level.WARNING, CLASS_NAME, "getJSPStrBuffer", "returning default ["+JSPStrBufferImpl.class.getName()+"]");
		        }
		        strBuf=new JSPStrBufferImpl();
			}
		}
		else {
	        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)) {
	            logger.logp(Level.WARNING, CLASS_NAME, "getJSPStrBuffer", "buf is null; JSPStrBufferFactory.set() must be called before getJSPStrBuffer() can be called.");
	            logger.logp(Level.WARNING, CLASS_NAME, "getJSPStrBuffer", "returning default ["+JSPStrBufferImpl.class.getName()+"]");
	        }
	        strBuf=new JSPStrBufferImpl();
		}
        return strBuf;
    }
}
