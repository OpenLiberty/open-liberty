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
package com.ibm.ws.jsp.webcontainerext;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.ibm.ws.jsp.Constants;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;

public class JspDependent {
	//	begin 213703: add logging for isoutdated checks
	private static Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.webcontainerext.JspDependent";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}
	//	end 213703: add logging for isoutdated checks

	
	static Pattern delimeter = Pattern.compile("\\" + Constants.TIMESTAMP_DELIMETER);
	
	
    long lastModified = -1;
    String dependentFilePath = null;
    JspTranslationContext context=null;
    private final String lineSep = (String)AccessController.doPrivileged(new PrivilegedAction() {
                                                                            public Object run() {
                                                                                return System.getProperty("line.separator");
                                                                            }
                                                                         });

        
    public JspDependent(String dependentFilePath, JspTranslationContext context) {
        this.context=context;
        
        String [] dependentItems = delimeter.split(dependentFilePath);
		if(dependentItems.length >1){
			this.dependentFilePath = dependentItems[0];
			Long ts = new Long (dependentItems[1]);
			this.lastModified = ts.longValue() ;				
		}
        
    }
    public boolean isOutdated() {
        Entry e = null;
        if (lastModified == -1 ) {
            return true;
        }
        boolean outdated = false;
        Container adaptableContainer = null;
        if (context.getServletContext()!=null) {
            adaptableContainer = context.getServletContext().getModuleContainer();
        }

        if (adaptableContainer!=null) {
            e = adaptableContainer.getEntry(dependentFilePath);
        }
        
        if (e!=null && e.getLastModified() != lastModified) {
                outdated=true;
                // begin 213703: add logging for isoutdated checks
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                        //logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "container ts [" + e.getLastModified() + "] differs from cached ts [" + this.lastModified +"]. Recompile JSP.");
                        logger.logp(Level.FINER, CLASS_NAME, "isOutdated", "container [" + dependentFilePath + "]");
                }
                // end 213703: add logging for isoutdated checks
            
        } else {
            File dependentFile = new File(context.getRealPath(dependentFilePath));
            long ts = dependentFile.lastModified();
            if (ts == 0) {ts = getTimestamp();}
            if (ts != lastModified){  
    			outdated = true;
    			// begin 213703: add logging for isoutdated checks
    			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
    				logger.logp(Level.FINER, CLASS_NAME, "isOutdated", "dependentFile ts [" + ts + "] differs from cached ts [" + this.lastModified +"]. Recompile JSP."); 
    				logger.logp(Level.FINER, CLASS_NAME, "isOutdated", "dependentFile [" + dependentFile + "]");
    			}
    			// end 213703: add logging for isoutdated checks
            }
        }                
        return outdated;                
    }
        
    public long getTimestamp() {
        if (lastModified == -1 ) {
            return 0;
        }
        Container adaptableContainer = null;
        if (context.getServletContext()!=null) {
            adaptableContainer = context.getServletContext().getModuleContainer();
        }
        if (adaptableContainer!=null) {
            Entry e = adaptableContainer.getEntry(dependentFilePath);
            if (e!=null) {
                return e.getLastModified();
            } else {
                //no entry, not sure we should ever get here
                return 0;
            }
        } else {
            File dependentFile = new File(context.getRealPath(dependentFilePath));
            return (dependentFile.lastModified());
        }
    }
        
    public String getDependentFilePath() {
        return (dependentFilePath);
    }
        
    public String toString(){
        return "JspDependent dependentFilePath = " + getDependentFilePath()+ lineSep+
                            "last modified = " + new java.util.Date(getTimestamp())+ lineSep+
                            "is outDated = " + isOutdated();
            
            
    }
}
