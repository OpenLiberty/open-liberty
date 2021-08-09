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

import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

/**
 * This is the service provider's interface (SPI) for plugging
 * in an external cache (eg, the AFPA cache, a web server cache,
 * a proxy server cache or a sprayer cache).
 * Each implementation of this interface encapsulates the particular
 * protocol supported by an external cache.
 * The CacheCoordinator calls this interface to manage pages
 * in external caches.  It is always called locally.
 *
 * <p>Restrictions on JSP fragments cached externally are the following:
 * <ul>
 *     <li>It must be a top-level fragment (ie, an externally requested page).
 *     <li>It must not have any security access restrictions.
 *         This restriction can be relaxed if the external cache
 *         supports some form of access control.
 *     <li>It must not need access statistics gathered.
 *         This restriction can be relaxed if the external cache
 *         supports some form of statistics gathering.
 * </ul>
 * @ibm-api 
 */
public interface ExternalCacheAdapter
{
    /**
     * This method sets the TCP/IP address of the cache adapter
     * @param address Address of the cache adapter
     * @ibm-api 
     */
    public void setAddress(String address);

    /**
     * This method writes pages to the external cache.
     *
     * @param externalCacheEntries The Enumeration of ExternalCacheEntry
     * objects for the pages that are to be cached.
     * @ibm-api 
     */
    public void writePages(Iterator externalCacheEntries);

    /**
     * This method invalidates pages that are in the external cache.
     *
     * @param urls The List of URLs for the pages that have
     * previously been written to the external cache and need invalidation.
     * @ibm-api 
     */
    public void invalidatePages(Iterator urls);

    /**
     * This method invalidates dependency ids that are in the external cache.
     *
     * @param ids The Enumeration of dependency ids that must be invalidated
     * @ibm-api 
     */
    public void invalidateIds(Iterator ids);

    /**
     * This method is invoked before processing a cache hit or miss
     * of an externally cacheable element
     *
     * @param sreq    The request object being used for this invocation
     * @param sresp   The response object being used for this invocation
     * @ibm-api 
     */
    public void preInvoke(ServletCacheRequest sreq, HttpServletResponse sresp);

    /**
     * This method is invoked after processing a cache hit or miss
     * of an externally cacheable element
     *
     * @param sreq    The request object being used for this invocation
     * @param sresp   The response object being used for this invocation
     * @ibm-api 
     */
    public void postInvoke(ServletCacheRequest sreq, HttpServletResponse sresp);

    /**
     * This method invalidates all pages from the external cache.
     * @ibm-api 
     */
    public void clear();
}
