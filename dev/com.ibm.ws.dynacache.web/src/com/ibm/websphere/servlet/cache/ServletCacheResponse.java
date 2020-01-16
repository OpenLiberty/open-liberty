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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * This interface is a proxy for the WebSphere response object.
 * It has features added to enable caching.
 * @ibm-api 
 */
public interface ServletCacheResponse extends HttpServletResponse
{
	/**
	 * This adds a Dynamic Content Provider that will
	 * generate dynamic content without executing its JSP.
	 *
	 * @param dynamicContentProvider The DynamicContentProvider.
         * @ibm-api 
	 */
	public void addDynamicContentProvider(DynamicContentProvider dynamicContentProvider) throws IOException;

        /**
         * This sets the page to not be consumed by its parents
         *      
         * @param value True if the page is to be set as do-not-consume
	 * @ibm-api 
         */
	public void setDoNotConsume(boolean doNotConsume);
 
}
