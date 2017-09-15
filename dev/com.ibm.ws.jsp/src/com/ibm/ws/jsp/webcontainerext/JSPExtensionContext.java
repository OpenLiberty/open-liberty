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
import java.io.FileNotFoundException;				//PM30435
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.inputsource.JspInputSourceFactoryImpl;
import com.ibm.ws.jsp.translator.resource.JspResourcesFactoryImpl;
import com.ibm.ws.webcontainer.util.DocumentRootUtils;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.jsp.compiler.JspCompilerFactory;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationEnvironment;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.JspInputSourceFactory;
import com.ibm.wsspi.jsp.resource.translation.JspResourcesFactory;

public class JSPExtensionContext implements JspTranslationContext {
    protected IServletContext context = null;
    protected DocumentRootUtils dru = null;
    protected URL contextURL = null;
    protected JspResourcesFactory jspResourcesFactory = null;
    protected JspInputSourceFactory jspInputSourceFactory = null;
    protected JspClassloaderContext jspClassloaderContext = null;
    protected JspCompilerFactory jspCompilerFactory = null;

    static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.webcontainerext.JSPExtensionContext";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    public JSPExtensionContext(IServletContext context,
    						   JspOptions jspOptions,
                               String extDocRoot,
                               String preFragExtDocRoot,
                               JspClassloaderContext jspClassloaderContext,
                               JspCompilerFactory jspCompilerFactory) {
        this.context = context;
        this.jspClassloaderContext = jspClassloaderContext;
        this.jspCompilerFactory = jspCompilerFactory;
        dru = new DocumentRootUtils(context, extDocRoot,preFragExtDocRoot);
        String docRoot = null;
        try {
            Container container = context.getModuleContainer();
            if (container==null) {
                docRoot = context.getRealPath("/");
                contextURL = new File(docRoot).toURL();
            } else {
                docRoot=null;
                //the contextURL should not be used when figuring paths out with containers
                contextURL = null;
                //Collection<URI> rootUris = container.getUri();
                //rootUris.iterator().next();
                //contextURL = new File("/").toURL();
            }
        }
        catch (MalformedURLException e) {
            logger.logp(Level.WARNING,"JSPExtensionContext","JSPExtensionContext", "Failed to create context URL for docRoot: "+ context.getRealPath("/") , e);
        }
        jspResourcesFactory = new JspResourcesFactoryImpl(jspOptions, this, context.getModuleContainer());
        jspInputSourceFactory = new JspInputSourceFactoryImpl(docRoot,contextURL, dru, false, context.getModuleContainer(), jspClassloaderContext.getClassLoader(),context);
        
    }

    public long getRealTimeStamp(String path) {
        JspInputSource inputSource = getJspInputSourceFactory().createJspInputSource(path);
        return inputSource.getLastModified();        
    }
    
    public String getRealPath(String path) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
            logger.entering(this.getClass().getName(), "getRealPath", path);
        }
        if (context != null) {
            String realPath = context.getRealPath(path);
            if (realPath == null || new java.io.File(realPath).exists() == false) {
                    //PK76503 add synchronized block
                    synchronized (dru) {
                        try {
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {  //PK76503 added trace
                                logger.logp(Level.FINE,CLASS_NAME,"getRealPath", "Checking extendedDocumentRoot path: " + path);
                            }
                            dru.handleDocumentRoots(path);
                            // return jar name or file name if not in jar.
                            realPath = dru.getFilePath();
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) { //PK76503 added trace
                                logger.logp(Level.FINE,CLASS_NAME,"getRealPath", "Path was retrieved from the extendedDoucumentRoots realPath: " + realPath);
                            }
                        }
                        // PM30435 start
                        catch (FileNotFoundException fne_io){
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                                logger.logp(Level.FINER,CLASS_NAME,"getRealPath", "FileNotFound exception while obtaining realPath: " +realPath + ", exception was: " + fne_io);
                            }
                            // this may happen if resource does not exist
                            // follow behavior from above and just return path below
                        } 
                        // PM30435 end
                        catch (Exception e) {
                            com.ibm.ws.ffdc.FFDCFilter.processException( e, "com.ibm.ws.jsp.webcontainerext.JspExtensionContext.getResourceAsStream", "102", this);
                            // follow behavior from above and just return path below
                        }
                    }
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                logger.exiting(this.getClass().getName(), "getRealPath", realPath);
            }
            return realPath;
        }
        else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                logger.exiting(this.getClass().getName(), "getRealPath", path);
            }
            return path;
        }
    }

    public java.util.Set getResourcePaths(String paths) {
        return context.getResourcePaths(paths);
    }
    
    public java.util.Set getResourcePaths(String path,boolean searchMetaInfResources) {   	
    	return context.getResourcePaths(path,searchMetaInfResources);
    }

    public JspResourcesFactory getJspResourcesFactory() {
        return jspResourcesFactory;
    }

    public JspInputSourceFactory getJspInputSourceFactory() {
        return jspInputSourceFactory;
    }

    public JspClassloaderContext getJspClassloaderContext() {
        return jspClassloaderContext;
    }

    public JspCompilerFactory getJspCompilerFactory() {
        return jspCompilerFactory;
    }

    public void setJspTranslationEnviroment(JspTranslationEnvironment jspEnvironment) {
    }
    
    /**
     * @return the servletContext
     */
    public IServletContext getServletContext() {
        return context;
    }

}
