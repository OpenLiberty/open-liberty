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
package com.ibm.ws.jsp.inputsource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletContext;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.webcontainer.util.DocumentRootUtils;

public class JspURLConnection extends URLConnection {
    private String relativeUrl = null;
    private DocumentRootUtils dru = null;
    private boolean searchOnClasspath = false;
    private ClassLoader classloader = null;
	private String docRoot;
	private ServletContext servletContext;
    
    public JspURLConnection(String docRoot, URL url, 
                            String relativeUrl, 
                            DocumentRootUtils dru, 
                            boolean searchOnClasspath,
                            ClassLoader classloader, ServletContext servletContext) {
        super(url);
        this.docRoot = docRoot;
        this.relativeUrl = relativeUrl;
        this.dru = dru;
        this.searchOnClasspath = searchOnClasspath;
        this.classloader = classloader;
        this.servletContext = servletContext;
        
        if (this.dru==null) {
    	    if (servletContext!=null) {
    		    this.dru = new DocumentRootUtils(servletContext,(String)null,(String)null);
    	    }
    	    else if (docRoot!=null){
    		    this.dru = new DocumentRootUtils(docRoot,(String)null,(String)null);
    	    }
        }
    }
    
    public void connect() throws IOException {}
    
    @FFDCIgnore(IOException.class)
    public InputStream getInputStream() throws IOException {
        InputStream is = null;
        try {
            URL newURL = new URL(url.toExternalForm()); 
            URLConnection conn = newURL.openConnection();
            conn.setUseCaches(false);
            is = conn.getInputStream();
        }
        catch (IOException e) {
            if (relativeUrl.endsWith(".tld") || relativeUrl.endsWith(".jar") || searchOnClasspath) {
                String s = relativeUrl;
                if (s.charAt(0) == '/')
                    s = s.substring(1);
                is = classloader.getResourceAsStream(s);
            }
        }
        
        if (is == null) {
        	if (dru!=null){
	            	//PK97121 start - add synchronized block
	            	synchronized (dru) {
	            		dru.handleDocumentRoots(relativeUrl);
	            		is = dru.getInputStream();
	            	}
	            	// PK97121 end
	        }
	        else {
	                throw new IOException(JspCoreException.getMsg("jsp.error.failed.to.find.resource", new Object[] {url}));
        	}
        }
        
        return is;
    }
}
