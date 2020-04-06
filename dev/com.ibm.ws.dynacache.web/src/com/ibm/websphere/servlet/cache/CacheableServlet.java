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
package com.ibm.websphere.servlet.cache;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

/**
 * This interface identifies cacheable servlets to the fragment cache. 
 * The cache will call the getId() and getSharingPolicy() methods to 
 * obtain the caching metadata for a given execution of the servlet.
 * @ibm-api 
 */
public interface CacheableServlet extends Serializable {

    /**
     * This executes the algorithm to compute the cache id.
     *
     * @param request The HTTP request object.
     * @return The cache id.  A null indicates that the servlet should 
     * not be cached.
     * @ibm-api 
     */
    public String getId(HttpServletRequest request);    

    /**
     * This returns the sharing policy for this cache entry.
     * See com.ibm.websphere.servlet.cache.EntryInfo for possible
     * values.
     *
     * @param request The HTTP request object.
     * @return The sharing policy
     * @ibm-api 
     */
    public int getSharingPolicy(HttpServletRequest request);
}
