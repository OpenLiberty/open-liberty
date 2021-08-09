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
package com.ibm.wsspi.jsp.context;

import com.ibm.wsspi.jsp.resource.JspInputSourceFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * Implementations of this interface are used by the JSP Container to
 * provide access to external resources and also additional Factory implements for
 * Input Source management and ClassLoader management. 
 */
public interface JspCoreContext {

    /**
     * Returns a long containing the last modified timestamp of the resource with the given virtual path
     * 
     * @param path The relative path of the resource
     * @return long the last modified timestamp of the resource 
     */
    long getRealTimeStamp(String path);
    
    /**
     * Returns a String containing the real path for a given virtual path
     * 
     * @param path The reletive path of the resource
     * @return String the real path to the resouce 
     */
    String getRealPath(String path);
    
    /**
     * Returns a directory-like listing of all the paths to resources 
     * within the web application whose longest sub-path matches the 
     * supplied path argument. The JSP Container uses this method to search 
     * for tld's in the war.
     * 
     * @param paths String - the partial path used to match the resources, 
     * which must start with a /
     * @param searchMetaInfResources boolean - indicates whether or not the 
     * the list should include paths from META-INF resources. 
     * @return Set - a Set containing the directory listing, or null if 
     * there are no resources in the web application whose path begins 
     * with the supplied path.
     */
    java.util.Set getResourcePaths(String paths, boolean searchMetaInfResources);
    
    /**
     * Returns a directory-like listing of all the paths to resources 
     * within the web application whose longest sub-path matches the 
     * supplied path argument. The JSP Container uses this method to search 
     * for tld's in the war.
     * 
     * @param paths String - the partial path used to match the resources, 
     * which must start with a /
     * @return Set - a Set containing the directory listing, or null if 
     * there are no resources in the web application whose path begins 
     * with the supplied path.
     */
    java.util.Set getResourcePaths(String paths);
    
    /**
     * Returns the JspInputSourceFactory object that the JSP Container will 
     * use to create InputSources.
     * 
     * @return JspInputSourceFactory
     */
    JspInputSourceFactory getJspInputSourceFactory();
    
    /**
     * Returns the JspInputSourceFactory object that the JSP Container will
     * use to load dependent classes and get the classpath to use for compiling.
     * 
     * @return JspClassloaderContext
     */
    JspClassloaderContext getJspClassloaderContext();
    
    public IServletContext getServletContext();
}
