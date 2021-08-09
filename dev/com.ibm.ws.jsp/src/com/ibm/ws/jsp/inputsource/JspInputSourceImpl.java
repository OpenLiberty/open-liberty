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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import com.ibm.wsspi.jsp.resource.JspInputSource;

public class JspInputSourceImpl implements JspInputSource {
	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.inputsource.JspInputSourceImpl";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    protected URL contextURL = null;
    protected URL absoluteURL = null;
    protected String relativeURL = null;
    protected URLStreamHandler urlStreamHandler = null;
    protected long lastModified = 0;
    protected Document document = null;
    
    public JspInputSourceImpl(URL contextURL, String relativeURL, URLStreamHandler urlStreamHandler) {
        this.contextURL = contextURL;
        this.relativeURL = relativeURL;
        this.urlStreamHandler = urlStreamHandler; 
        String resolvedRelativeURL = relativeURL;
        if (resolvedRelativeURL.charAt(0) == '/') {
            resolvedRelativeURL = resolvedRelativeURL.substring(1);
        }
        try {
            if (urlStreamHandler != null) {
                absoluteURL = new URL(contextURL, resolvedRelativeURL, urlStreamHandler);
            }
            else {
                absoluteURL = new URL(contextURL, resolvedRelativeURL);
            }
        }
        catch (MalformedURLException e) {
			logger.logp(Level.WARNING, CLASS_NAME, "JspInputSourceImpl", "Failed to create inputsource contextURL =[" + contextURL +" relativeURL =[" + relativeURL +"]", e);
        }
    }
    
    public JspInputSourceImpl(JspInputSourceImpl baseImpl, String relativeURL, URLStreamHandler urlStreamHandler) {
        this(baseImpl.contextURL, relativeURL, urlStreamHandler);        
    }
    
    public URL getAbsoluteURL() {
        return absoluteURL;
    }

    public URL getContextURL() {
        return contextURL;
    }

    public Document getDocument() {
        return document;
    }

    public InputStream getInputStream() throws IOException {
        InputStream is = null;
        
        URLConnection conn = absoluteURL.openConnection();
        conn.setUseCaches(false);
        is = conn.getInputStream();
        lastModified = conn.getLastModified();
        return is;   
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getRelativeURL() {
        return relativeURL;
    }

    public boolean isXmlDocument() {
        return (document != null);
    }

}
