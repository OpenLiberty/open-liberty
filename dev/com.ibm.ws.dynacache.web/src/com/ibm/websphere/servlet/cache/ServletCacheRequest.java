/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.servlet.cache;

import java.util.*;
import javax.servlet.http.*;

/**
 * This interface is a proxy for the WebSphere request object.
 * It has features added to enable caching.
 * @ibm-api 
 */
public interface ServletCacheRequest extends HttpServletRequest
{
    
    /**
     * This returns the FragmentInfo for this entry,
     * which contains the caching metadata for the entry.
     *
     * @return The caching metadata for this entry.
    * @ibm-api 
     */
    public FragmentInfo getFragmentInfo();
    /**
     * This gets the include variable.
     *
     * @return True indicates that the include call was used to
     * create this fragment.  False indicates that the forward call was used.
     * @ibm-api 
     */
    public boolean getInclude();
    

    /**
     * This returns a Hashtable containing the request attributes
     * as they were just prior to exectuion of the entry.
     * It also creates the Hashtable if it did not already exist.
     *
     * @return The hashtable of attributes.
     * @ibm-api 
     */
    public Hashtable getAttributeTable();

    /**
     * This returns the request attribute with the specified key.
     * It overrides the method in the WebSphere request.
     *
     * @param key The attribute key.
     * @return The attribute value.
     * @ibm-api 
     */
    public Object getAttribute(String key) ;

    /**
     * This sets the request attribute key-value pair.
     * It overrides the method in the WebSphere request.
     *
     * @param key The attribute key.
     * @param value The attribute value.
     * @ibm-api 
     */
    public void setAttribute(String key, Object value);
        
    /**
     * This sets the page to be uncachebale
     *      
     * @param value True if the page is to be set as uncacheable
     * @ibm-api 
     */
    public void setUncacheable(boolean value);

    /**
     * This returns true if the page is uncacheable
     *
     * @return True indicates that the fragment is uncacheable and
     * false indicates that the fragment is cacheable.
     * @ibm-api 
     */
    public boolean isUncacheable();
    
    /**
     * This method prepares the javax.servlet.ServletInputStream to be read by the IdGenerator.
     * <pre>
     * Usage example: 
     *   servletCacheRequest.setGeneratingId(true);
     *   InputStream in = servletCacheRequest.getInputStream();
     *              :
     *   servletCacheRequest.setGeneratingId(false);
     * </pre>
     * @param b  True or false
     * @ibm-api 
     */
    public void setGeneratingId(boolean b);
}
