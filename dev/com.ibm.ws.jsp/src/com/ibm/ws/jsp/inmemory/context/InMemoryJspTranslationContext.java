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
package com.ibm.ws.jsp.inmemory.context;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.inmemory.compiler.InMemoryJspCompilerFactory;
import com.ibm.ws.jsp.inmemory.resource.InMemoryJspResourceFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.jsp.compiler.JspCompilerFactory;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationEnvironment;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.JspInputSourceFactory;
import com.ibm.wsspi.jsp.resource.translation.JspResourcesFactory;

public class InMemoryJspTranslationContext implements JspTranslationContext {

    static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.inmemory.context.InMemoryJspTranslationContext";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    private IServletContext servletContext = null;
    private JspTranslationEnvironment jspEnvironment = null;
    private JspResourcesFactory resourceFactory = null;
    private JspCompilerFactory compilerFactory = null;
    private JspOptions jspOptions = null;
    //protected DocumentRootUtils dru = null;
    
    public InMemoryJspTranslationContext(IServletContext servletContext, JspOptions jspOptions, String extDocRoot, String preFragExtDocRoot) {
        this.servletContext = servletContext;
        this.jspOptions = jspOptions;
        //dru = new DocumentRootUtils(servletContext,extDocRoot,preFragExtDocRoot);
    }

    public JspCompilerFactory getJspCompilerFactory() {
        if (compilerFactory == null) {
            compilerFactory = new InMemoryJspCompilerFactory(jspEnvironment.getDefaultJspClassloaderContext().getClassLoader(), jspOptions);
        }
        return compilerFactory;
    }

    public JspResourcesFactory getJspResourcesFactory() {
        if (resourceFactory == null) {
            resourceFactory = new InMemoryJspResourceFactory(this, jspEnvironment);
        }
        return resourceFactory;
    }

    public void setJspTranslationEnviroment(JspTranslationEnvironment jspEnvironment) {
        this.jspEnvironment = jspEnvironment;
    }

    public JspClassloaderContext getJspClassloaderContext() {
        return jspEnvironment.getDefaultJspClassloaderContext();
    }

    public JspInputSourceFactory getJspInputSourceFactory() {
        return jspEnvironment.getDefaultJspInputSourceFactory();
    }

    public long getRealTimeStamp(String path) {
        JspInputSource inputSource = getJspInputSourceFactory().createJspInputSource(path);
        return inputSource.getLastModified();        
    }
    
    public String getRealPath(String path) {
        if (servletContext != null) {
            String realPath = servletContext.getRealPath(path);
            //servletContext.getRealPath now checks the edr root according to checkEDRInGetRealpath
            /*if (new java.io.File(realPath).exists() == false) {
	            synchronized (dru) {
	                try {
	                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {  //PK76503 added trace
	                        logger.logp(Level.FINE,CLASS_NAME,"getRealPath", "Checking extendedDocumentRoot path: " + path);
	                    }
	                    dru.handleDocumentRoots(path);
	                    realPath = dru.getFilePath();
	                    if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) { //PK76503 added trace
	                        logger.logp(Level.FINE,CLASS_NAME,"getRealPath", "Path was retrieved from the extendedDoucumentRoots realPath: " + realPath);
	                    }
	                }
	                catch (Exception fne_io) {
	                   com.ibm.ws.ffdc.FFDCFilter.processException( fne_io, "com.ibm.ws.jsp.webcontainerext.JspExtensionContext.getResourceAsStream", "93", this);
	                   // this may happen if resource does not exist
	                   // follow behavior from above and just return path below
	                }
            	}
            } */    
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                logger.exiting(CLASS_NAME, "getRealPath", realPath);
            }
            return realPath;
        }
        else {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                logger.exiting(CLASS_NAME, "getRealPath", path);
            }
            return path;
        }
        //return servletContext.getRealPath(path);
    }
    
    public java.util.Set getResourcePaths(String path,boolean searchMetaInfResources) {   	
    	return servletContext.getResourcePaths(path,searchMetaInfResources);
    }


    public Set getResourcePaths(String paths) {
        return servletContext.getResourcePaths(paths);
    }
    
    /**
     * @return the servletContext
     */
    public IServletContext getServletContext() {
        return servletContext;
    }

    /**
     * @param servletContext the servletContext to set
     */
    public void setServletContext(IServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
