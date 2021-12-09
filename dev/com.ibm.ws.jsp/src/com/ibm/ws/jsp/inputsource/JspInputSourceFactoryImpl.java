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

import java.net.URL;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.ServletContext;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer.util.DocumentRootUtils;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.JspInputSourceFactory;

public class JspInputSourceFactoryImpl implements JspInputSourceFactory {
    protected URL contextURL = null;
    protected DocumentRootUtils dru = null;
    protected boolean searchClasspathForResources = false;
    protected ClassLoader classloader = null;
    private String docRoot;
    private ServletContext servletContext;
    private Container container;
    

    public JspInputSourceFactoryImpl(String docRoot, URL contextURL, 
                                     DocumentRootUtils dru,
                                     boolean searchClasspathForResources, 
                                     Container container,
                                     ClassLoader classloader) {
        this(docRoot, contextURL, dru, searchClasspathForResources, container, classloader, null);
    }
    
    public JspInputSourceFactoryImpl(String docRoot, URL contextURL, 
            DocumentRootUtils dru,
            boolean searchClasspathForResources, 
            Container container,
            ClassLoader classloader,
            ServletContext servletContext) {
        this.docRoot = docRoot;
        this.contextURL = contextURL;
        this.dru = dru;
        this.searchClasspathForResources = searchClasspathForResources;
        this.container = container;
        this.classloader = classloader;
        this.servletContext = servletContext;
    }
    @FFDCIgnore(ClassCastException.class)
    public JspInputSource copyJspInputSource(JspInputSource base, String relativeURL) {
        JspInputSource jspInputSource = null;
        Container localContainer = container;
        if(container != null){ //If container is null, we won't ever have a JspInputSourceContainerImpl. No container logic
            try {
                localContainer = ((JspInputSourceContainerImpl)base).getContainer(); //Look for the file in the given container. If not, just use the primary (probably the war)
            } catch(ClassCastException cce) {
                //FFDCIgnore. Just use the primary container
            }
        }
        if (System.getSecurityManager() != null) {
            final String finalJspRelativeUrl = relativeURL; 
            final JspInputSource finalBase = base;
            final ServletContext finalServletContext = servletContext;
            final Container finalLocalContainer = localContainer;
            jspInputSource = (JspInputSource)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    URLStreamHandler urlStreamHandler = null;
                    if (finalLocalContainer==null) {
                        urlStreamHandler = new JspURLStreamHandler(docRoot,finalJspRelativeUrl, 
                                                                                dru, 
                                                                                searchClasspathForResources, 
                                                                                classloader,
                                                                                finalServletContext);
                        return new JspInputSourceImpl((JspInputSourceImpl)finalBase, finalJspRelativeUrl, urlStreamHandler);
                    } else {
                        return new JspInputSourceContainerImpl(finalLocalContainer, finalJspRelativeUrl, urlStreamHandler, dru);
                    }
                }
            });
        }
        else {
            URLStreamHandler urlStreamHandler = null;
            if (container==null) {
                urlStreamHandler = new JspURLStreamHandler(docRoot,
                                                           relativeURL, 
                                                           dru,
                                                           searchClasspathForResources, 
                                                           classloader,
                                                           servletContext);
                jspInputSource = new JspInputSourceImpl((JspInputSourceImpl)base, relativeURL, urlStreamHandler);
            } else {
                jspInputSource = new JspInputSourceContainerImpl(localContainer, relativeURL, urlStreamHandler, dru);
            }
        }
            
        return jspInputSource; 
    }

    public JspInputSource createJspInputSource(String relativeURL) {
        JspInputSource jspInputSource = null;
        
        if (System.getSecurityManager() != null) {
            final String finalJspRelativeUrl = relativeURL; 
            final ServletContext finalServletContext = servletContext;
            jspInputSource = (JspInputSource)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    URLStreamHandler urlStreamHandler = null;
                    if (container==null) {
                        urlStreamHandler = new JspURLStreamHandler(docRoot,finalJspRelativeUrl, 
                                                                                dru, 
                                                                                searchClasspathForResources, 
                                                                                classloader,
                                                                                finalServletContext);
                        return new JspInputSourceImpl(contextURL, finalJspRelativeUrl, urlStreamHandler);
                    } else {
                        return new JspInputSourceContainerImpl(container, finalJspRelativeUrl, urlStreamHandler, dru);
                    }
                }
            });
        }
        else {
            URLStreamHandler urlStreamHandler = null;
            if (container==null) {
                urlStreamHandler = new JspURLStreamHandler(docRoot,relativeURL, 
                                                                        dru,
                                                                        searchClasspathForResources, 
                                                                        classloader,
                                                                        servletContext);
                jspInputSource = new JspInputSourceImpl(contextURL, relativeURL, urlStreamHandler);
            } else {
                jspInputSource = new JspInputSourceContainerImpl(container, relativeURL, urlStreamHandler, dru);
            }
        }
            
        return jspInputSource; 
    }

    public JspInputSource createJspInputSource(URL contextURL, String relativeURL) {
        if (container!=null) {
            return createJspInputSource(relativeURL);
        }
        JspInputSource jspInputSource = null;
        
        if (System.getSecurityManager() != null) {
            final String finalJspRelativeUrl = relativeURL; 
            final URL finalContextURL = contextURL;
            final ServletContext finalServletContext = servletContext;
            jspInputSource = (JspInputSource)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    URLStreamHandler urlStreamHandler = new JspURLStreamHandler(docRoot,
                                                                                finalJspRelativeUrl, 
                                                                                dru, 
                                                                                searchClasspathForResources, 
                                                                                classloader,
                                                                                finalServletContext);
                    return new JspInputSourceImpl(finalContextURL, finalJspRelativeUrl, urlStreamHandler);
                }
            });
        }
        else {
            URLStreamHandler urlStreamHandler = new JspURLStreamHandler(docRoot,
                                                                        relativeURL, 
                                                                        dru,
                                                                        searchClasspathForResources, 
                                                                        classloader,
                                                                        servletContext);
            jspInputSource = new JspInputSourceImpl(contextURL, relativeURL, urlStreamHandler);
        }
            
        return jspInputSource; 
    }

}
