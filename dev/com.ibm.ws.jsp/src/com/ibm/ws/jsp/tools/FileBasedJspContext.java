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
package com.ibm.ws.jsp.tools;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.inputsource.JspInputSourceFactoryImpl;
import com.ibm.ws.jsp.translator.compiler.JDTCompilerFactory;
import com.ibm.ws.jsp.translator.compiler.JspCompilerFactoryImpl;
import com.ibm.ws.jsp.translator.resource.JspResourcesFactoryImpl;
import com.ibm.ws.webcontainer.util.DocumentRootUtils;
import com.ibm.wsspi.jsp.compiler.JspCompilerFactory;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationEnvironment;
import com.ibm.wsspi.jsp.resource.JspInputSourceFactory;
import com.ibm.wsspi.jsp.resource.translation.JspResourcesFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public class FileBasedJspContext implements JspTranslationContext {

	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.tools.FileBasedJspContext";
	static {
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    private String docRoot = "";
	private boolean searchClasspathForResources = false; // defect 201520
    private DocumentRootUtils dru;
    private URL contextURL = null;
    private JspResourcesFactory jspResourcesFactory = null;
    private JspInputSourceFactory jspInputSourceFactory = null;
    private JspClassloaderContext jspClassloaderContext = null;
    private JspCompilerFactory jspCompilerFactory = null;
    //protected IServletContext servletContext = null;
    
	/**
     * @return the servletContext
     */
    public IServletContext getServletContext() {
        return null;
    }

    // defect 201520 - add searchClasspathForResources
    //public FileBasedJspContext(IServletContext context, String docRoot, JspOptions jspOptions, String extDocRoot, String preFragExtDocRoot, ClassLoader loader, JspClassloaderContext jspClassloaderContext, boolean searchClasspathForResources) {
    public FileBasedJspContext(String docRoot, JspOptions jspOptions, String extDocRoot, String preFragExtDocRoot, ClassLoader loader, JspClassloaderContext jspClassloaderContext, boolean searchClasspathForResources) {
        //this.servletContext = context;
        this.docRoot = docRoot;
        this.searchClasspathForResources=searchClasspathForResources;

        File baseDir = new File(docRoot);
        baseDir = baseDir.getParentFile();
        dru = new DocumentRootUtils(baseDir.toString(), extDocRoot,preFragExtDocRoot);

        try {
            contextURL = new URL("file", null, getRealPath("/"));
        }
        catch (MalformedURLException e) {
			logger.logp(Level.WARNING, CLASS_NAME, "FileBasedJspContext", "Failed to create context URL for docRoot: " + docRoot, e);
        }
        jspResourcesFactory = new JspResourcesFactoryImpl(jspOptions, this, null);
        // defect 201520 - add searchClasspathForResources instead of hardcoded true
        jspInputSourceFactory = new JspInputSourceFactoryImpl(docRoot,contextURL, dru, this.searchClasspathForResources, null, loader);
        this.jspClassloaderContext = jspClassloaderContext;
        if (jspOptions.isUseJDKCompiler()) {
        	jspCompilerFactory = new JspCompilerFactoryImpl(getRealPath("/"), jspClassloaderContext, jspOptions);
        }
        else {
        	jspCompilerFactory = new JDTCompilerFactory(jspClassloaderContext.getClassLoader(), jspOptions);
        }
    }

    public String getRealPath(String path) {
        return (getRealPath(path, true));
    }
    
    public long getRealTimeStamp(String path) {
        File f = new File(getRealPath(path));
        return f.lastModified();
    }

    public String getRealPath(String path, boolean checkExtDocRoot) {
        String realPath = "";
        if (path.startsWith("/"))
            realPath = docRoot + path;
        else
            realPath = docRoot + "/" + path;
        
        if (checkExtDocRoot) {
            if ((new File(realPath).exists() == false) && dru != null && dru.searchPathExists()) {
                try {
                	//PK97121 start - add synchronized block
                	synchronized (dru) {
                		dru.handleDocumentRoots(path);
                	    realPath = dru.getFilePath();

                	} //PK97121 end
                }
                catch (Exception fne_io) {
                    // this may happen if resource does not exist
                    // follow behavior from above and just return path below
                }
            }
        }
        return (realPath);
    }
    
    public java.util.Set getResourcePaths(String path,boolean SearchMetsInfResources) {   	
    	// this object never searched META-INF resources so ignore the booelan
    	return this.getResourcePaths(path);
    }


    public java.util.Set getResourcePaths(String path) {
        java.util.HashSet set = new java.util.HashSet();

        java.io.File root = new java.io.File(docRoot + path);

        if (root.exists()) {
            java.io.File[] fileList = root.listFiles();
            if (fileList != null) {
                for (int i = 0; i < fileList.length; i++) {
                    String resourcePath = fileList[i].getPath();
                    resourcePath = resourcePath.substring(docRoot.length());
                    resourcePath = resourcePath.replace('\\', '/');
                    if (fileList[i].isDirectory()) {
                        if (resourcePath.endsWith("/") == false) {
                            resourcePath += "/";
                        }
                    }
                    set.add(resourcePath);
                }
            }
        }
        return (set);
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
}
