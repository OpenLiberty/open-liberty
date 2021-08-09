/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.cache.EntryInfo;
import com.ibm.websphere.servlet.cache.CacheableServlet;

public class CacheableServletImpl extends HttpServlet implements CacheableServlet
{
    private static final long serialVersionUID = -8913065289974983288L;
    
    /**
     * This implements the method in the CacheableServlet interface.
     *
     * @param request The HTTP request object.
     * @return The cache id.  A null indicates that the JSP should 
     * not be cached.
     */
    public String getId(HttpServletRequest request)
    {
        return null;
    }    

    /**
     * This implements the method in the CacheableServlet interface.
     *
     * @param request The HTTP request object.
     * @return The cache id.
     */
    public int getSharingPolicy(HttpServletRequest request)
    {
        return EntryInfo.NOT_SHARED;
    }
}
