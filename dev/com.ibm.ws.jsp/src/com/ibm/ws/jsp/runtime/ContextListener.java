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
package com.ibm.ws.jsp.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.taglib.annotation.AnnotationHandler;

public class ContextListener implements ServletContextListener {
    static private Logger logger;
    private boolean isUseThreadTagPool=false;
    private static final String CLASS_NAME="com.ibm.ws.jsp.runtime.ContextListener";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    //method used to set whether we are using Thread Tag Pooling.  We don't have access to the jsp options here.
    public void setIsUseThreadTagPool(boolean b) {
        isUseThreadTagPool = b;
    }
    public void contextDestroyed(ServletContextEvent event) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINER)){
         	logger.entering(CLASS_NAME,"contextDestroyed", "event = [" + event+"] for context: ["+event.getServletContext().getServletContextName()+"]");
	}//d651265
        if (isUseThreadTagPool) {
            Map webAppPoolMap = (Map)event.getServletContext().getAttribute(Constants.TAG_THREADPOOL_MAP);
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, "contextDestroyed", "webAppPoolMap = [" + webAppPoolMap+"]");
            }
            if (webAppPoolMap != null) {
                for (Iterator itr = webAppPoolMap.keySet().iterator(); itr.hasNext();) {
                    Integer threadId = (Integer)itr.next();
                    Map m = (Map)webAppPoolMap.get(threadId);
                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                        logger.logp(Level.FINER, CLASS_NAME, "contextDestroyed", "threadId = [" + threadId+"]");
                        logger.logp(Level.FINER, CLASS_NAME, "contextDestroyed", "map from webAppPoolMap = [" + m+"]");
                    }
                    if (m!=null) {
    	                for (Iterator itr2 = m.keySet().iterator(); itr2.hasNext();) {
    	                    String tagKey = (String)itr2.next();
    	                    TagArray tagArray = (TagArray)m.get(tagKey);
    	                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
    	                        logger.logp(Level.FINER, CLASS_NAME, "contextDestroyed", "tagKey = [" + tagKey+"]");
    	                        logger.logp(Level.FINER, CLASS_NAME, "contextDestroyed", "tagArray = [" + tagArray+"]");
    	                    }
    	                    if (tagArray!=null) {
    		                    tagArray.releaseTags();
    		                    tagArray = null;     
    		                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
    		                        logger.logp(Level.FINER, CLASS_NAME, "contextDestroyed", "released tags and set tagArray to null");
    		                    }
    	                    }
    	                }
    	                m.clear();
    	                m = null;
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                            logger.logp(Level.FINER, CLASS_NAME, "contextDestroyed", "cleared map and set map to null");
                        }
                    }
                }
                webAppPoolMap.clear();
                webAppPoolMap = null;
                event.getServletContext().removeAttribute(Constants.TAG_THREADPOOL_MAP);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                    logger.logp(Level.FINER, CLASS_NAME, "contextDestroyed", "cleared webAppPoolMap, set webAppPoolMap to null and removed attribute from servletcontext ["+Constants.TAG_THREADPOOL_MAP+"]");
                }
            }
        }
        //removing annotationHandler so that there aren't any references to the stopped ServletContext's classLoader
        AnnotationHandler annotationHandlerRemoved = AnnotationHandler.removeAnnotationHandler(event.getServletContext());
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
            if (annotationHandlerRemoved!=null) {
                logger.exiting(CLASS_NAME,"contextDestroyed", "cleared Annotation for Servlet Context."); //d651265
            } else {
                logger.exiting(CLASS_NAME,"contextDestroyed", "no Annotation for Servlet Context found."); //d651265
            }
        }
    }

    public void contextInitialized(ServletContextEvent event) {
        if (isUseThreadTagPool) {
            event.getServletContext().setAttribute(Constants.TAG_THREADPOOL_MAP, new HashMap());                                                                    
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, "contextInitialized", "set attribute on servletcontext = [" + Constants.TAG_THREADPOOL_MAP+"]  for context: ["+event.getServletContext().getServletContextName()+"]");
            }
        }
    }
}
