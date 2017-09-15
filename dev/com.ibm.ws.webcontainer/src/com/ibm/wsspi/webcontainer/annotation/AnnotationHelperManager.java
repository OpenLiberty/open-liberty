/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.annotation;

import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.wsspi.webcontainer.facade.ServletContextFacade;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 *
 */
public class AnnotationHelperManager {

    /* need to support
     * com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager _jspx_aHelper = 
           com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager.getInstance(config.getServletContext());
     
       com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager _jspx_aHelper =
           com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager.getInstance(getServletConfig().getServletContext());
  
       _jspx_iaHelper = _jspx_aHelper.getAnnotationHelper()

       _jspx_iaHelper.inject(tagStartWriter.print (tagHandlerVar) ; );
       _jspx_iaHelper.doPostConstruct(tagStartWriter.print (tagHandlerVar); );
  
     */

    
    private static Map<ServletContext, AnnotationHelperManager>
          servletContextToAnnotationHelperManagerMap = new Hashtable<ServletContext, AnnotationHelperManager>();

    
    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.annotation");
    protected static final String CLASS_NAME = "com.ibm.wsspi.webcontainer.annotation.AnnotationHelperManager";

    private WebApp wApp = null;
    private AnnotationHelper aHelper = null;
    
    
    public static void addInstance(ServletContext context,  AnnotationHelperManager annoHelperMgr) {

        AnnotationHelperManager.servletContextToAnnotationHelperManagerMap.put(context,annoHelperMgr);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp (Level.FINE, CLASS_NAME, "addInstance", "added instance with key: " + context);
       }

    }

    public static void removeInstance(ServletContext context) {

        AnnotationHelperManager.servletContextToAnnotationHelperManagerMap.remove(context);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp (Level.FINE, CLASS_NAME, "removeInstance", "remove instance with key: " + context);
       }
    }


    public AnnotationHelperManager(ServletContext iApp) {
        try { 
            wApp = (WebApp) iApp;
        } catch (Exception e) {
            // need to handle this more better 
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp (Level.FINE, CLASS_NAME, "constructor", "can set up webApp in AnnotationHelperManager constructor.  Exception: " + e);
           }
            
        }
    }

    
    public static synchronized AnnotationHelperManager getInstance(ServletContext context) {

        IServletContext servletContext =
            AnnotationHelperManager.unwrapServletContext (context);

        AnnotationHelperManager instance =
            AnnotationHelperManager.servletContextToAnnotationHelperManagerMap.get(servletContext);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp (Level.FINE, CLASS_NAME, "getInstance",
                 "context: " + context + "  gave AHM instance --> [" + instance + "]");
       }
       
       return instance;
    }
 
    
    private static IServletContext unwrapServletContext
    (ServletContext context) {
   if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
            logger.logp (Level.FINE, CLASS_NAME,
                 "unwrapServletContext", "original context->"+context);
       }
    

     while (context instanceof ServletContextFacade){
             if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
             logger.logp (Level.FINE, CLASS_NAME,
                  "unwrapServletContext", "nested context->"+context);
         }
             context = ((ServletContextFacade)context).getIServletContext();
     }
    
    return (IServletContext)context;
}

    
    /**
     * Retrieves an AnnotationHelper instance.
     * 
     * @return an AnnotationHelper object containing the desired instance.
     */
    public  synchronized AnnotationHelper getAnnotationHelper () {

        if (aHelper == null) {
            this.aHelper = new AnnotationHelper(wApp);    
    
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)) {
                logger.logp (Level.FINE, CLASS_NAME, "getAnnotationHelper",
                     "created new annotation helper instance --> [" + this.aHelper + "]");
            }
        }
        
        return aHelper;
    }
    
    /**
     * This method can be called to ensure that the class has been loaded. See defect 96420. 
     */
    public static void verifyClassIsLoaded () { 
        // No op
    }
}