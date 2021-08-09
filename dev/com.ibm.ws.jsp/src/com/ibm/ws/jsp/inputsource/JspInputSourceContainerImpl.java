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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import com.ibm.ws.webcontainer.util.DocumentRootUtils;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.jsp.resource.JspInputSource;

public class JspInputSourceContainerImpl implements JspInputSource {
	static private Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.inputsource.JspInputSourceImpl";
	static{
		logger = Logger.getLogger("com.ibm.ws.jsp");
	}

    protected Container container = null;
    protected Entry inputSourceEntry = null;
    protected URL contextURL = null;
    protected URL absoluteURL = null;
    protected String relativeURL = null;
    protected URLStreamHandler urlStreamHandler = null;
    protected long lastModified = 0;
    protected Document document = null;
    protected DocumentRootUtils dru = null;
    
    public JspInputSourceContainerImpl(Container container, String relativeURL, URLStreamHandler urlStreamHandler, DocumentRootUtils dru) {
        this.container = container;
        this.relativeURL = relativeURL;
        this.urlStreamHandler = urlStreamHandler; 
        this.inputSourceEntry = container.getEntry(relativeURL);
        this.dru = dru;
        if (this.inputSourceEntry!=null) {
            this.lastModified = this.inputSourceEntry.getLastModified();
        }
        /*String resolvedRelativeURL = relativeURL;
        if (resolvedRelativeURL.charAt(0) == '/') {
            resolvedRelativeURL = resolvedRelativeURL.substring(1);
        }
        try {
            Entry e = container.getEntry(relativeURL);
            URI u = null;
            if (e!=null) {
                Collection<URI> collection = e.convertToContainer().getUri();
                Iterator<URI> it = collection.iterator();
                if (it.hasNext()) {
                    u = it.next();
                }
            }
            if (u!=null) {
                contextURL = new URL(u.toString());
                if (urlStreamHandler != null) {
                    absoluteURL = new URL(contextURL, resolvedRelativeURL, urlStreamHandler);
                }
                    //absoluteURL = new URL(contextURL, resolvedRelativeURL, urlStreamHandler);
                else {
                    absoluteURL = new URL(contextURL, resolvedRelativeURL);
                }
            }
        }
        catch (MalformedURLException e) {
			logger.logp(Level.WARNING, CLASS_NAME, "JspInputSourceImpl", "Failed to create inputsource contextURL =[" + contextURL +" relativeURL =[" + relativeURL +"]", e);
        }*/
    }
    
    //public JspInputSourceContainerImpl(JspInputSourceContainerImpl baseImpl, String relativeURL, URLStreamHandler urlStreamHandler) {
    //    this(baseImpl.contextURL, relativeURL, urlStreamHandler);        
    //}
    
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
        if (inputSourceEntry!=null) {
            lastModified = inputSourceEntry.getLastModified();
            try {
                return inputSourceEntry.adapt(InputStream.class);
            } catch (UnableToAdaptException ex) {
                throw new IllegalStateException(ex);
            }
        } else {
            try {
                if(dru!=null){
                    synchronized(dru){
                        dru.handleDocumentRoots(relativeURL);
                        InputStream is = dru.getInputStream();
                        this.lastModified = dru.getLastModified();
                        return is;
                    }
                }
            } catch (FileNotFoundException e) {
                //no-op the file wasn't found
            } catch (IOException e){
                //no-op the file wasn't found
            }
            return null;
        }
    }

    public long getLastModified() {
        if(inputSourceEntry == null && dru != null){
            try {
                synchronized(dru){
                    dru.handleDocumentRoots(relativeURL);
                    this.lastModified = dru.getLastModified();
                }
            } catch (FileNotFoundException e) {
                return 0;
            } catch (IOException e){
                return 0;
            }
        }
        else if (inputSourceEntry != null) {
            return inputSourceEntry.getLastModified();
        }
        return lastModified;
    }

    public String getRelativeURL() {
        return relativeURL;
    }

    public boolean isXmlDocument() {
        return (document != null);
    }
    
    public Container getContainer() {
        return container;
    }
    
    public Entry getInputSourceEntry(){
        return inputSourceEntry;
    }
}
