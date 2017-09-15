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
package com.ibm.ws.jsp.lite;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.translator.utils.NameMangler;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;

public class JSPLiteExtProcessor extends com.ibm.ws.webcontainer.extension.WebExtensionProcessor {
    
    protected static final int numSyncObjects = 41;
    protected static Object[] syncObjects;
    static {
        syncObjects = new Object[numSyncObjects];
        for (int i = 0; i < numSyncObjects; i++) syncObjects[i] = new Object();
    }

    static final protected Object getSyncObject(String name) {
        return syncObjects[Math.abs(name.hashCode() % numSyncObjects)];
    }
    
    public JSPLiteExtProcessor(IServletContext webapp) throws Exception {
        super(webapp);
    }

    public IServletWrapper createServletWrapper(IServletConfig config) throws Exception {
        JSPLiteExtServletWrapper jspServletWrapper = new JSPLiteExtServletWrapper(extensionContext);
        jspServletWrapper.initialize(config);
        return jspServletWrapper;
    }

    public void handleRequest(ServletRequest req, ServletResponse res) throws Exception {
        if (req instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) req;
            
            IServletWrapper jspServletWrapper = findWrapper(httpReq, (HttpServletResponse)res);
            if (jspServletWrapper != null) {
                jspServletWrapper.handleRequest(req, res);
            }
        }
    }
    
    private IServletWrapper findWrapper(HttpServletRequest httpReq, HttpServletResponse res) throws Exception {
        IServletWrapper jspServletWrapper = null;
        IServletConfig sconfig = getConfig(httpReq);

        String filename = sconfig.getFileName();
            
        synchronized (getSyncObject(filename)) {
            RequestProcessor rp = extensionContext.getMappingTarget(filename);
            if (rp instanceof ExtensionProcessor) {
                jspServletWrapper = createServletWrapper(sconfig);
                extensionContext.addMappingTarget(filename, jspServletWrapper);
                
                try {
                	extensionContext.addMappingTarget(filename, jspServletWrapper);
                }
                catch (Exception e) {
                	extensionContext.replaceMappingTarget(filename, jspServletWrapper);
                }
            }
            else {
                jspServletWrapper = (IServletWrapper)rp;    
            }
        }
        
        return jspServletWrapper;         
    }

    public List getPatternList() {
        return Collections.EMPTY_LIST;    
    }

    public IServletConfig getConfig(HttpServletRequest req) throws ServletException {
        String includeUri = (String) req.getAttribute(Constants.INC_SERVLET_PATH);

        String jspUri = null;

        if (includeUri == null) {
            jspUri = req.getServletPath();
            if (req.getPathInfo() != null) {
            	String pathInfo = req.getPathInfo();
            	int semicolon = pathInfo.indexOf(';');
                if (semicolon != -1){
                    pathInfo = pathInfo.substring(0, semicolon);
                }
                jspUri += pathInfo;
            }
        }
        else {
            String pathInfo = (String) req.getAttribute(Constants.INC_PATH_INFO);
            jspUri = includeUri;
            if (pathInfo != null) {
                int semicolon = pathInfo.indexOf(';');
                if (semicolon != -1){
                    pathInfo = pathInfo.substring(0, semicolon);
                }
                jspUri += pathInfo;
            }
        }
        
        jspUri = com.ibm.ws.util.WSUtil.resolveURI(jspUri);
        IServletConfig sconfig = createConfig(jspUri);
        sconfig.setServletName(jspUri);
        sconfig.setDisplayName(jspUri);
        sconfig.setServletContext(extensionContext);
        sconfig.setIsJsp(false);
        sconfig.setInitParams(new HashMap());
        sconfig.setFileName(jspUri);
        sconfig.setClassName(getPackageName(jspUri) + "." + getClassName(jspUri));
        
        return sconfig;
    }

    private String getPackageName(String jspUri) {
        String packageName=jspUri.substring(0,jspUri.lastIndexOf("/")+1);
        if ( !packageName.equals("/") ) {
            packageName = NameMangler.handlePackageName (packageName);
            packageName=Constants.JSP_PACKAGE_PREFIX+"."+packageName.substring(0,packageName.length()-1);
        }
        else {
            packageName=Constants.JSP_PACKAGE_PREFIX;
        }
        return packageName;
    }
    
    private String getClassName(String jspUri) {
        if (jspUri.charAt(0) != '/') {
            jspUri = "/"+jspUri;
        }
        return NameMangler.mangleClassName(jspUri);
    }
    
	public boolean isAvailable(String resource) {
		String resourcePath = getPackageName(resource) + "." + getClassName(resource);
		resourcePath = resourcePath.replace('.', '/');
		java.io.File resourceFile = new java.io.File(extensionContext.getRealPath(resourcePath));//PM21451
		return resourceFile.exists();
	}

	public IServletWrapper getServletWrapper() {
		// TODO Auto-generated method stub
		return null;
	}

	public WebComponentMetaData getMetaData() {
		// TODO Auto-generated method stub
		return null;
	}

	public IServletWrapper getServletWrapper(ServletRequest req,
			ServletResponse resp) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
