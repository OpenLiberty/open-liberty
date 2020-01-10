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
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;

/**
 * Implement this interface in your Servlet or JSP to
 * have a callback invoked during cache hits.  This
 * will allow Dynamic Cache to imbed your dynamic content 
 * within a cached fragment.
 * @ibm-api 
 */
public interface DynamicContentProvider {

	/**
	  * This method generates and writes the dynamic content to the OutputStream.
	  * It is called on a cache hit or miss to generate the dynamic content of the cacheable servlet.
	  * @param request      The HttpServletRequest to determin what dynamic content to create.
	  * @param streamWriter The OutputStream that this method will write the dynamic content to.
	  * 
      * @ibm-api 
	  */
	public void provideDynamicContent(HttpServletRequest request, OutputStream streamWriter) throws IOException;

	/**
	 * This method generates and writes the dynamic content to the Writer.
	 * It is called on a cache hit or miss to generate the dynamic content of the cacheable servlet.
	 * @param request      The HttpServletRequest to determin what dynamic content to create.
	 * @param streamWriter The Writer that this method will write the dynamic content to.
	 * 
     * @ibm-api 
	 */
	public void provideDynamicContent(HttpServletRequest request, Writer streamWriter) throws IOException;
}
