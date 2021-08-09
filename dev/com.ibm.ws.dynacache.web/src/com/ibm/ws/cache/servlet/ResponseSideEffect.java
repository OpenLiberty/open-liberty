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
package com.ibm.ws.cache.servlet;

import java.io.Serializable;

import javax.servlet.http.HttpServletResponse;

/**
 * When a JSP must be executed without its parent JSP being executed
 * (i.e., the child is not in the cache but the parent was in the cache),
 * the side effect of any code in the parent that changes the response
 * object is also cached so that the response can be put back in the 
 * correct state for child execution. 
 * This interface provides a method to apply the side effect to the 
 * response object.  
 * This interface is supported in the AddCookieSideEffect,
 * ContentLengthSideEffect, ContentTypeSideEffect, DateHeaderSideEffect,
 * DefaultStatusSideEffect, HeaderSideEffect and StatusSideEffect
 * classes. 
 */
public interface ResponseSideEffect 
extends Serializable
{
    /**
     * This executes the side effect on the response object. 
     * 
     * @param response The response object that the side effect applies to.
     */
    public void
    performSideEffect(HttpServletResponse response);
}
